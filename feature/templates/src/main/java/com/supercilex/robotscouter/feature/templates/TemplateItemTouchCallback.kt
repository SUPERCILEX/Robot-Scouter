package com.supercilex.robotscouter.feature.templates

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.view.postOnAnimationDelayed
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.common.ChangeEventType
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.google.android.material.appbar.AppBarLayout
import com.google.firebase.firestore.WriteBatch
import com.supercilex.robotscouter.common.FIRESTORE_POSITION
import com.supercilex.robotscouter.core.LateinitVal
import com.supercilex.robotscouter.core.data.firestoreBatch
import com.supercilex.robotscouter.core.data.logFailures
import com.supercilex.robotscouter.core.model.OrderedRemoteModel
import com.supercilex.robotscouter.core.ui.isItemInRange
import com.supercilex.robotscouter.core.ui.longSnackbar
import com.supercilex.robotscouter.core.ui.maxAnimationDuration
import com.supercilex.robotscouter.core.ui.showKeyboard
import com.supercilex.robotscouter.core.ui.swap
import com.supercilex.robotscouter.feature.templates.viewholder.TemplateViewHolder
import java.util.Collections
import kotlin.math.roundToInt
import com.supercilex.robotscouter.R as RC

internal class TemplateItemTouchCallback<T : OrderedRemoteModel>(
        private val rootView: View
) : ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN,
        ItemTouchHelper.START
) {
    private val recyclerView: RecyclerView = rootView.findViewById(RC.id.metricsView)
    private val appBar: AppBarLayout =
            (rootView.context as FragmentActivity).findViewById(RC.id.appBar)
    var adapter: FirestoreRecyclerAdapter<T, *> by LateinitVal()
    var itemTouchHelper: ItemTouchHelper by LateinitVal()

    private val deleteIcon: Drawable = checkNotNull(AppCompatResources.getDrawable(
            rootView.context, R.drawable.ic_delete_black_24dp))
    private val deletePaint = Paint().apply {
        color = ContextCompat.getColor(rootView.context, RC.color.delete_background)
    }
    private val deleteIconPadding = rootView.resources.getDimensionPixelSize(RC.dimen.spacing_large)

    private val localItems = mutableListOf<T>()
    private var animatorPointer: RecyclerView.ItemAnimator? = null
    private var scrollToPosition = RecyclerView.NO_POSITION
    private var isMovingItem = false
    private var isDeletingItem = false

    fun getItem(position: Int): T = if (isMovingItem || isDeletingItem) {
        localItems[position]
    } else {
        adapter.snapshots[position]
    }

    fun getItemCount(injectedSuperCall: () -> Int): Int = if (isMovingItem || isDeletingItem) {
        localItems.size
    } else {
        injectedSuperCall()
    }

    fun onBind(viewHolder: RecyclerView.ViewHolder, position: Int) {
        viewHolder as TemplateViewHolder

        viewHolder.enableDragToReorder(viewHolder, itemTouchHelper)

        if (position == scrollToPosition) {
            // Posting to the main thread b/c the fam covers the screen which makes the LLM think
            // there's only one item
            recyclerView.post {
                if (recyclerView.isItemInRange(position)) {
                    viewHolder.requestFocus()
                    viewHolder.nameEditor.showKeyboard()
                } else {
                    // Scroll a second time to account for the ViewHolder not having been added when the
                    // first scroll occurred.
                    recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                        override fun onScrollStateChanged(
                                recyclerView: RecyclerView,
                                newState: Int
                        ) {
                            if (newState != RecyclerView.SCROLL_STATE_IDLE) return

                            recyclerView.removeOnScrollListener(this)
                            (recyclerView.findViewHolderForLayoutPosition(position) as? TemplateViewHolder)?.apply {
                                requestFocus()
                                nameEditor.showKeyboard()
                            }
                        }
                    })
                    recyclerView.smoothScrollToPosition(position)
                }
            }

            scrollToPosition = RecyclerView.NO_POSITION
        }
    }

    fun addItemToScrollQueue(position: Int) {
        scrollToPosition = position
    }

    fun onChildChanged(
            type: ChangeEventType,
            newIndex: Int,
            oldIndex: Int,
            injectedSuperCall: () -> Unit
    ) {
        if (isMovingItem && isDeletingItem) {
            isMovingItem = false
            isDeletingItem = false
            cleanupFailure()
            return
        }

        if (isMovingItem) {
            if (isCatchingUpOnMove(type, newIndex)) {
                if (adapter.snapshots == localItems) {
                    postCleanup { isMovingItem = false }
                }

                // Update item corners
                adapter.notifyItemChanged(newIndex)
                adapter.notifyItemChanged(oldIndex)
            } else {
                isMovingItem = false
                cleanupFailure()
            }
        } else if (isDeletingItem) {
            if (isCatchingUpOnDelete(type, newIndex)) {
                if (adapter.snapshots == localItems) {
                    postCleanup { isDeletingItem = false }
                }

                if (oldIndex != -1) {
                    // Update item corners
                    adapter.notifyItemChanged(oldIndex)
                    adapter.notifyItemChanged(oldIndex - 1)
                }
            } else {
                isDeletingItem = false
                cleanupFailure()
            }
        } else if (type == ChangeEventType.ADDED && newIndex == scrollToPosition) {
            injectedSuperCall()
            recyclerView.post {
                appBar.setExpanded(false)
                recyclerView.smoothScrollToPosition(newIndex)
            }
        } else {
            injectedSuperCall()
        }
    }

    private fun isCatchingUpOnMove(
            type: ChangeEventType,
            index: Int
    ): Boolean = type != ChangeEventType.REMOVED &&
            (type == ChangeEventType.MOVED || hasOnlyPositionChanged(type, index))

    private fun isCatchingUpOnDelete(type: ChangeEventType, index: Int): Boolean {
        if (type == ChangeEventType.REMOVED || type == ChangeEventType.MOVED) {
            // Account for move events when we update lower item positions
            return true
        } else if (type == ChangeEventType.CHANGED) {
            if (hasOnlyPositionChanged(type, index)) return true

            val path = adapter.snapshots[index].ref.path
            // Is this change event just an update to the deleted item?
            if (localItems.none { it.ref.path == path }) return true
        }
        return false
    }

    private fun hasOnlyPositionChanged(type: ChangeEventType, index: Int): Boolean {
        val updatedModel = adapter.snapshots[index]
        val originalModelPosition = updatedModel.position

        val hasOnlyPositionChanged = type == ChangeEventType.CHANGED && localItems.any {
            it == updatedModel.apply { position = it.position }
        }
        updatedModel.position = originalModelPosition

        return hasOnlyPositionChanged
    }

    private inline fun postCleanup(crossinline cleanup: () -> Unit) {
        recyclerView.postOnAnimationDelayed(
                (animatorPointer ?: recyclerView.itemAnimator).maxAnimationDuration()
        ) { cleanup(); this.cleanup() }
    }

    private fun cleanupFailure() {
        cleanup()
        rootView.longSnackbar(R.string.template_move_cancelled_rationale)
        adapter.notifyDataSetChanged()
    }

    override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
    ): Boolean {
        if (!isMovingItem) localItems.addAll(adapter.snapshots)
        isMovingItem = true

        adapter.swap(viewHolder, target) { i, j ->
            localItems[i].position = j
            localItems[j].position = i
            Collections.swap(localItems, i, j)
        }

        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        if (!isDeletingItem) localItems.addAll(adapter.snapshots)
        isDeletingItem = true
        val localItems = localItems.toList()

        val position = viewHolder.adapterPosition
        val deletedRef = localItems[position].ref
        val itemsBelow: List<OrderedRemoteModel> = localItems.subList(position + 1, localItems.size)

        this.localItems.removeAt(position)
        adapter.notifyItemRemoved(position)

        recyclerView.clearFocus() // Save user data for undo
        deletedRef.get().addOnSuccessListener(rootView.context as FragmentActivity) { snapshot ->
            firestoreBatch {
                updatePositions(itemsBelow, -1)
                delete(deletedRef)
            }.logFailures("onSwiped:deleteMetric", deletedRef, itemsBelow)

            rootView.longSnackbar(RC.string.deleted, RC.string.undo) {
                firestoreBatch {
                    set(deletedRef, checkNotNull(snapshot.data))
                    updatePositions(itemsBelow, 1)
                }.logFailures("onSwiped:addMetric", deletedRef, itemsBelow)
            }
        }.logFailures("onSwiped:getMetric", deletedRef)
    }

    override fun onChildDraw(
            c: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean
    ) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        if (actionState != ItemTouchHelper.ACTION_STATE_SWIPE) return

        val v = viewHolder.itemView

        c.drawRect(
                v.right.toFloat() + dX,
                v.top.toFloat(),
                v.right.toFloat(),
                v.bottom.toFloat(),
                deletePaint
        )
        deleteIcon.apply {
            val right = v.right - deleteIconPadding
            val center = (v.height / 2.0).roundToInt()
            val half = intrinsicHeight / 2
            setBounds(
                    right - intrinsicWidth,
                    v.top + center - half,
                    right,
                    v.bottom - center + half
            )
            draw(c)
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        if (isMovingItem) {
            recyclerView.itemAnimator?.let { animatorPointer = it }
            recyclerView.post { recyclerView.itemAnimator = null }
            firestoreBatch {
                updatePositions(localItems)
            }.logFailures("clearView:updatePositions", localItems.map { it.ref })
        }
    }

    private fun WriteBatch.updatePositions(items: List<OrderedRemoteModel>, offset: Int = 0) {
        for (item in items) {
            item.position += offset
            update(item.ref, FIRESTORE_POSITION, item.position)
        }
    }

    private fun cleanup() {
        localItems.clear()
        animatorPointer?.let { recyclerView.itemAnimator = it }
        animatorPointer = null
    }

    override fun isLongPressDragEnabled() = false
}

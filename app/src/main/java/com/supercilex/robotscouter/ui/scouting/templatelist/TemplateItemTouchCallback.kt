package com.supercilex.robotscouter.ui.scouting.templatelist

import android.arch.core.executor.ArchTaskExecutor
import android.graphics.Canvas
import android.graphics.Paint
import android.support.design.widget.AppBarLayout
import android.support.graphics.drawable.VectorDrawableCompat
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewCompat
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.View
import com.firebase.ui.common.ChangeEventType
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.google.firebase.firestore.WriteBatch
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.OrderedRemoteModel
import com.supercilex.robotscouter.ui.scouting.templatelist.viewholder.TemplateViewHolder
import com.supercilex.robotscouter.util.FIRESTORE_POSITION
import com.supercilex.robotscouter.util.LateinitVal
import com.supercilex.robotscouter.util.data.firestoreBatch
import com.supercilex.robotscouter.util.logFailures
import com.supercilex.robotscouter.util.ui.isItemInRange
import com.supercilex.robotscouter.util.ui.maxAnimationDuration
import com.supercilex.robotscouter.util.ui.showKeyboard
import com.supercilex.robotscouter.util.ui.swap
import kotterknife.bindView
import org.jetbrains.anko.design.longSnackbar
import java.util.Collections
import kotlin.math.roundToInt

class TemplateItemTouchCallback<T : OrderedRemoteModel>(
        private val rootView: View
) : ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN,
        ItemTouchHelper.START
) {
    private val recyclerView: RecyclerView by rootView.bindView(R.id.list)
    private val appBar: AppBarLayout by (rootView.context as FragmentActivity).bindView(R.id.app_bar)
    var adapter: FirestoreRecyclerAdapter<T, *> by LateinitVal()
    var itemTouchHelper: ItemTouchHelper by LateinitVal()

    private val deleteIcon = VectorDrawableCompat.create(
            rootView.resources, R.drawable.ic_delete_black_24dp, rootView.context.theme)!!
    private val deletePaint = Paint().apply {
        color = ContextCompat.getColor(rootView.context, R.color.delete_background)
    }
    private val deleteIconPadding =
            rootView.resources.getDimension(R.dimen.spacing_large).toInt()

    private val localItems = ArrayList<T>()
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
            ArchTaskExecutor.getInstance().postToMainThread {
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

    private fun isCatchingUpOnMove(type: ChangeEventType, index: Int): Boolean =
            type == ChangeEventType.MOVED || hasOnlyPositionChanged(type, index)

    private fun isCatchingUpOnDelete(type: ChangeEventType, index: Int): Boolean {
        if (type == ChangeEventType.REMOVED || type == ChangeEventType.MOVED) {
            // Account for move events when we update lower item positions
            return true
        } else if (type == ChangeEventType.CHANGED) {
            if (hasOnlyPositionChanged(type, index)) return true

            val path = adapter.snapshots[index].ref.path
            // Is this change event just an update to the deleted item?
            if (localItems.find { it.ref.path == path } == null) return true
        }
        return false
    }

    private fun hasOnlyPositionChanged(type: ChangeEventType, index: Int): Boolean {
        val updatedModel = adapter.snapshots[index]
        val originalModelPosition = updatedModel.position

        val hasOnlyPositionChanged = type == ChangeEventType.CHANGED && localItems.find {
            it == updatedModel.apply { position = it.position }
        } != null
        updatedModel.position = originalModelPosition

        return hasOnlyPositionChanged
    }

    private inline fun postCleanup(crossinline cleanup: () -> Unit) {
        ViewCompat.postOnAnimationDelayed(
                recyclerView,
                { cleanup(); this.cleanup() },
                (animatorPointer ?: recyclerView.itemAnimator).maxAnimationDuration())
    }

    private fun cleanupFailure() {
        cleanup()
        longSnackbar(rootView, R.string.template_move_cancelled_rationale)
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

        val position = viewHolder.adapterPosition
        val deletedRef = localItems[position].ref
        val itemsBelow: List<OrderedRemoteModel> =
                localItems.subList(position + 1, localItems.size).toList()

        localItems.removeAt(position)
        adapter.notifyItemRemoved(position)

        recyclerView.clearFocus() // Save user data for undo
        deletedRef.get().addOnSuccessListener(rootView.context as FragmentActivity) { snapshot ->
            firestoreBatch {
                delete(deletedRef)
                updatePositions(itemsBelow, -1)
            }.logFailures()

            longSnackbar(rootView, R.string.deleted, R.string.undo) {
                firestoreBatch {
                    updatePositions(itemsBelow, 1)
                    set(deletedRef, snapshot.data)
                }.logFailures()
            }
        }.logFailures()
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
            animatorPointer = recyclerView.itemAnimator
            recyclerView.itemAnimator = null
            firestoreBatch {
                updatePositions(localItems)
            }.logFailures()
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

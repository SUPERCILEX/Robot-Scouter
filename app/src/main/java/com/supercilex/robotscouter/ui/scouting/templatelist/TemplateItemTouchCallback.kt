package com.supercilex.robotscouter.ui.scouting.templatelist

import android.support.design.widget.AppBarLayout
import android.support.v4.app.FragmentActivity
import android.support.v4.view.ViewCompat
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.MotionEvent
import android.view.View
import com.firebase.ui.common.ChangeEventType
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.google.firebase.firestore.DocumentSnapshot
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.OrderedModel
import com.supercilex.robotscouter.ui.scouting.templatelist.viewholder.TemplateViewHolder
import com.supercilex.robotscouter.util.FIRESTORE_POSITION
import com.supercilex.robotscouter.util.LateinitVal
import com.supercilex.robotscouter.util.data.firestoreBatch
import com.supercilex.robotscouter.util.ui.maxAnimationDuration
import kotterknife.bindView
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.find
import java.util.Collections

class TemplateItemTouchCallback<T : OrderedModel>(private val rootView: View) : ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.LEFT) {
    private val recyclerView: RecyclerView by rootView.bindView(R.id.list)
    private val appBar: AppBarLayout by (rootView.context as FragmentActivity).bindView(R.id.app_bar)
    var adapter: FirestoreRecyclerAdapter<T, *> by LateinitVal()
    var itemTouchHelper: ItemTouchHelper by LateinitVal()

    private val movableItems = ArrayList<T>()
    private var animatorPointer: RecyclerView.ItemAnimator? = null
    private var scrollToPosition = RecyclerView.NO_POSITION
    private var isMovingItem = false

    fun getItem(position: Int): T =
            if (isMovingItem) movableItems[position] else adapter.snapshots[position]

    fun onBind(viewHolder: RecyclerView.ViewHolder, position: Int) {
        viewHolder.itemView.find<View>(R.id.reorder)
                .setOnTouchListener(View.OnTouchListener { v, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        viewHolder.itemView.clearFocus() // Saves data
                        itemTouchHelper.startDrag(viewHolder)
                        v.performClick()
                        return@OnTouchListener true
                    }
                    false
                })

        if (position == scrollToPosition) {
            // Scroll a second time to account for the ViewHolder not having been added when the
            // first scroll occurred.
            recyclerView.smoothScrollToPosition(position)
            recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState != RecyclerView.SCROLL_STATE_IDLE) return
                    recyclerView.removeOnScrollListener(this)
                    (recyclerView.findViewHolderForLayoutPosition(position) as? TemplateViewHolder)
                            ?.requestFocus()
                }
            })
            scrollToPosition = RecyclerView.NO_POSITION
        }
    }

    fun addItemToScrollQueue(position: Int) {
        scrollToPosition = position
    }

    fun onChildChanged(
            type: ChangeEventType, newIndex: Int, oldIndex: Int, injectedSuperCall: () -> Unit) {
        if (isMovingItem) {
            if (isCatchingUpOnMove(type, newIndex)) {
                if (adapter.snapshots == movableItems) {
                    ViewCompat.postOnAnimationDelayed(
                            recyclerView,
                            { cleanupMove() },
                            maxAnimationDuration(animatorPointer ?: recyclerView.itemAnimator))
                }

                // Update item corners
                adapter.notifyItemChanged(newIndex)
                adapter.notifyItemChanged(oldIndex)
            } else {
                cleanupMove()
                longSnackbar(rootView, R.string.template_move_cancelled_rationale)
                adapter.notifyDataSetChanged()
            }
            return
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
            type == ChangeEventType.MOVED
                    // Setting `position` causes an update.
                    // Our model doesn't include positions in its equals implementation
                    || type == ChangeEventType.CHANGED && movableItems.contains(adapter.snapshots[index])

    override fun onMove(recyclerView: RecyclerView,
                        viewHolder: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder): Boolean {
        val fromPos = viewHolder.adapterPosition
        val toPos = target.adapterPosition

        if (!isMovingItem) movableItems.addAll(adapter.snapshots)
        isMovingItem = true

        if (fromPos < toPos) {
            for (i in fromPos until toPos) swapDown(i)
        } else {
            for (i in fromPos downTo toPos + 1) swapUp(i)
        }
        adapter.notifyItemMoved(fromPos, toPos)

        return true
    }

    private fun swapDown(i: Int) = swap(i, i + 1)

    private fun swapUp(i: Int) = swap(i, i - 1)

    private fun swap(i: Int, j: Int) {
        movableItems[i].position = j
        movableItems[j].position = i
        Collections.swap(movableItems, i, j)
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val deletedRef = adapter.snapshots.getSnapshot(viewHolder.adapterPosition).reference

        viewHolder.itemView.clearFocus() // Needed to prevent the item from being re-added
        deletedRef.get().addOnSuccessListener { snapshot: DocumentSnapshot ->
            deletedRef.delete()

            longSnackbar(rootView, R.string.deleted, R.string.undo) {
                deletedRef.set(snapshot.data)
            }
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        if (isMovingItem) {
            animatorPointer = recyclerView.itemAnimator
            recyclerView.itemAnimator = null
            firestoreBatch {
                for (item in movableItems) {
                    update(item.ref, FIRESTORE_POSITION, item.position)
                }
            }
        }
    }

    private fun cleanupMove() {
        isMovingItem = false
        movableItems.clear()
        animatorPointer?.let { recyclerView.itemAnimator = it }
        animatorPointer = null
    }

    override fun isLongPressDragEnabled() = false
}

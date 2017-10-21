package com.supercilex.robotscouter.ui.scouting.templatelist

import android.support.design.widget.Snackbar
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
import java.util.Collections

class TemplateItemTouchCallback<T : OrderedModel>(private val rootView: View) : ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.LEFT) {
    private val recyclerView: RecyclerView by rootView.bindView(R.id.list)
    var adapter: FirestoreRecyclerAdapter<T, *> by LateinitVal()
    var itemTouchHelper: ItemTouchHelper by LateinitVal()

    private val movableItems = ArrayList<T>()
    private var animatorPointer: RecyclerView.ItemAnimator? = null
    private var scrollToPosition = RecyclerView.NO_POSITION
    private var isMovingItem = false

    fun getItem(position: Int): T =
            if (isMovingItem) movableItems[position] else adapter.snapshots[position]

    fun onBind(viewHolder: RecyclerView.ViewHolder, position: Int) {
        viewHolder.itemView.findViewById<View>(R.id.reorder)
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
            (viewHolder as TemplateViewHolder).requestFocus()
            scrollToPosition = RecyclerView.NO_POSITION
        }
    }

    fun addItemToScrollQueue(position: Int) {
        scrollToPosition = position
    }

    fun onChildChanged(type: ChangeEventType, index: Int, injectedSuperCall: () -> Unit) {
        if (isMovingItem) {
            if (run isCatchingUpOnMove@ {
                if (type == ChangeEventType.MOVED) return@isCatchingUpOnMove true
                else {
                    if (type == ChangeEventType.CHANGED
                            && movableItems.contains(adapter.snapshots[index])) {
                        return@isCatchingUpOnMove true
                    }
                }

                return@isCatchingUpOnMove false
            }) {
                injectedSuperCall()
                if (adapter.snapshots == movableItems) {
                    ViewCompat.postOnAnimationDelayed(
                            recyclerView,
                            { cleanupMove() },
                            maxAnimationDuration(animatorPointer ?: recyclerView.itemAnimator))
                }
            } else {
                cleanupMove()
                Snackbar.make(rootView, R.string.move_cancelled, Snackbar.LENGTH_LONG).show()
                adapter.notifyDataSetChanged()
            }
            return
        } else if (type == ChangeEventType.ADDED && index == scrollToPosition) {
            recyclerView.smoothScrollToPosition(scrollToPosition)
        }
        injectedSuperCall()
    }

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

            Snackbar.make(rootView, R.string.deleted, Snackbar.LENGTH_LONG)
                    .setAction(R.string.undo) { deletedRef.set(snapshot.data) }
                    .show()
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

        // We can't directly update the background because the header metric needs to update its padding
        adapter.notifyItemChanged(viewHolder.layoutPosition)
    }

    private fun cleanupMove() {
        isMovingItem = false
        movableItems.clear()
        animatorPointer?.let { recyclerView.itemAnimator = it }
        animatorPointer = null
    }
}

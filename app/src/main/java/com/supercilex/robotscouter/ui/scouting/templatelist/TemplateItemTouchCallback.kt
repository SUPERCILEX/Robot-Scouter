package com.supercilex.robotscouter.ui.scouting.templatelist

import android.support.design.widget.Snackbar
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.MotionEvent
import android.view.View
import com.firebase.ui.database.ChangeEventListener
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.google.firebase.crash.FirebaseCrash
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.ui.scouting.templatelist.viewholder.TemplateViewHolder
import java.util.Collections

class TemplateItemTouchCallback<T>(private val rootView: View) :
        ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.LEFT) {
    private val recyclerView: RecyclerView = rootView.findViewById(R.id.list)
    lateinit var adapter: FirebaseRecyclerAdapter<T, *>
    lateinit var itemTouchHelper: ItemTouchHelper

    private val movableSnapshots = ArrayList<DataSnapshot>()
    private var scrollToPosition = RecyclerView.NO_POSITION
    private var isMovingItem = false

    fun getItem(position: Int): T {
        val snapshots = adapter.snapshots
        return if (isMovingItem) snapshots.getObject(snapshots.indexOf(movableSnapshots[position]))
        else snapshots.getObject(position)
    }

    fun getRef(position: Int): DatabaseReference =
            (if (isMovingItem) movableSnapshots[position] else adapter.snapshots[position]).ref

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

    fun onChildChanged(type: ChangeEventListener.EventType, index: Int): Boolean {
        if (isMovingItem) {
            cleanupMove()
            Snackbar.make(rootView, R.string.move_cancelled, Snackbar.LENGTH_LONG).show()
            adapter.notifyDataSetChanged()
            return false
        } else if (type == ChangeEventListener.EventType.ADDED && index == scrollToPosition) {
            recyclerView.scrollToPosition(scrollToPosition)
        }
        return true
    }

    override fun onMove(recyclerView: RecyclerView,
                        viewHolder: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder): Boolean {
        val fromPos = viewHolder.adapterPosition
        val toPos = target.adapterPosition

        if (!isMovingItem) movableSnapshots.addAll(adapter.snapshots)
        isMovingItem = true

        if (fromPos < toPos) {
            for (i in fromPos until toPos) Collections.swap(movableSnapshots, i, i + 1)
        } else {
            for (i in fromPos downTo toPos + 1) Collections.swap(movableSnapshots, i, i - 1)
        }
        adapter.notifyItemMoved(fromPos, toPos)

        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.adapterPosition
        val deletedRef: DatabaseReference = adapter.getRef(position)

        viewHolder.itemView.clearFocus() // Needed to prevent the item from being re-added
        deletedRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                deletedRef.removeValue()

                Snackbar.make(rootView, R.string.item_deleted, Snackbar.LENGTH_LONG)
                        .setAction(R.string.undo) { deletedRef.setValue(snapshot.value, position) }
                        .show()
            }

            override fun onCancelled(error: DatabaseError) = FirebaseCrash.report(error.toException())
        })
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        if (isMovingItem) {
            for ((i, snapshot) in movableSnapshots.withIndex()) snapshot.ref.setPriority(i)
            cleanupMove()
        }

        // We can't directly update the background because the header metric needs to update its padding
        adapter.notifyItemChanged(viewHolder.layoutPosition)
    }

    private fun cleanupMove() {
        isMovingItem = false
        movableSnapshots.clear()
    }
}

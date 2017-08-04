package com.supercilex.robotscouter.ui.scouting.template

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
import com.supercilex.robotscouter.ui.scouting.template.viewholder.TemplateViewHolder
import com.supercilex.robotscouter.util.ui.CardListHelper

class TemplateItemTouchCallback(private val rootView: View) :
        ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.LEFT) {
    private val recyclerView: RecyclerView = rootView.findViewById(R.id.list)
    private lateinit var adapter: FirebaseRecyclerAdapter<*, *>
    private lateinit var itemTouchHelper: ItemTouchHelper
    private lateinit var cardListHelper: CardListHelper

    private var startScrollPosition = RecyclerView.NO_POSITION
    private var scrollToPosition = RecyclerView.NO_POSITION
    private var isItemMoving = false

    fun setItemTouchHelper(itemTouchHelper: ItemTouchHelper) {
        this.itemTouchHelper = itemTouchHelper
    }

    fun setAdapter(adapter: FirebaseRecyclerAdapter<*, *>) {
        this.adapter = adapter
    }

    fun setCardListHelper(cardListHelper: CardListHelper) {
        this.cardListHelper = cardListHelper
    }

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
        if (isItemMoving) {
            return type == ChangeEventListener.EventType.MOVED
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

        if (!isItemMoving) startScrollPosition = fromPos
        isItemMoving = true

        val fromRef = adapter.getRef(fromPos)
        if (toPos > fromPos) {
            for (i in fromPos + 1..toPos) adapter.getRef(i).setPriority(i - 1)
        } else {
            for (i in fromPos - 1 downTo toPos) adapter.getRef(i).setPriority(i + 1)
        }
        fromRef.setPriority(toPos)

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
        // We can't directly update the background because the header metric needs to update its padding
        adapter.notifyItemChanged(startScrollPosition)
        adapter.notifyItemChanged(viewHolder.layoutPosition)

        isItemMoving = false
        startScrollPosition = RecyclerView.NO_POSITION
    }
}

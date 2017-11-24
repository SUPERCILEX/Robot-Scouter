package com.supercilex.robotscouter.ui.scouting.templatelist.viewholder

import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.MotionEvent
import android.view.View
import android.widget.EditText

interface TemplateViewHolder : View.OnFocusChangeListener {
    val reorder: View
    val nameEditor: EditText

    fun init() {
        nameEditor.onFocusChangeListener = this
    }

    fun requestFocus() {
        nameEditor.requestFocus()
    }

    fun enableDragToReorder(viewHolder: RecyclerView.ViewHolder, helper: ItemTouchHelper) {
        reorder.setOnTouchListener(View.OnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                viewHolder.itemView.clearFocus() // Saves data
                helper.startDrag(viewHolder)
                v.performClick()
                return@OnTouchListener true
            }
            false
        })
    }
}

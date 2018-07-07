package com.supercilex.robotscouter.feature.templates.viewholder

import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

internal interface TemplateViewHolder : View.OnFocusChangeListener {
    val reorderView: View
    val nameEditor: EditText

    fun init() {
        nameEditor.onFocusChangeListener = this
    }

    fun requestFocus() {
        nameEditor.requestFocus()
    }

    fun enableDragToReorder(
            viewHolder: RecyclerView.ViewHolder,
            helper: ItemTouchHelper
    ) = reorderView.setOnTouchListener(View.OnTouchListener { v, event ->
        if (event.action == MotionEvent.ACTION_DOWN) {
            viewHolder.itemView.clearFocus() // Saves data
            helper.startDrag(viewHolder)
            v.performClick()
            return@OnTouchListener true
        }
        false
    })
}

package com.supercilex.robotscouter.ui.scouting.templatelist.viewholder

import android.support.annotation.CallSuper
import android.view.View
import android.widget.EditText

interface TemplateViewHolder : View.OnFocusChangeListener {
    val reorder: View
    val nameEditor: EditText

    @CallSuper
    fun bind() {
        nameEditor.onFocusChangeListener = this
    }

    fun requestFocus() {
        nameEditor.requestFocus()
    }
}

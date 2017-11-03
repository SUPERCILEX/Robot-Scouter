package com.supercilex.robotscouter.ui.scouting.templatelist.viewholder

import android.view.View
import android.widget.EditText

interface TemplateViewHolder : View.OnFocusChangeListener {
    val reorder: View
    val nameEditor: EditText

    fun requestFocus() {
        nameEditor.requestFocus()
    }
}

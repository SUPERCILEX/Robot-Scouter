package com.supercilex.robotscouter.ui.scout.viewholder.template

import android.support.annotation.Keep
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.View
import android.widget.EditText

import com.google.firebase.database.DataSnapshot
import com.supercilex.robotscouter.R

class SpinnerItemViewHolder @Keep constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView), ScoutTemplateViewHolder {
    private val itemEditText: EditText = itemView.findViewById(R.id.name) as EditText

    private lateinit var prevText: String
    private lateinit var snapshot: DataSnapshot

    fun bind(text: String, snapshot: DataSnapshot) {
        prevText = text
        this.snapshot = snapshot

        itemEditText.setText(text)
        itemEditText.onFocusChangeListener = this
    }

    override fun requestFocus() {
        itemEditText.requestFocus()
    }

    override fun onFocusChange(v: View, hasFocus: Boolean) {
        val text: String = itemEditText.text.toString()
        if (!hasFocus && !TextUtils.equals(text, prevText)) {
            snapshot.ref.setValue(text, snapshot.priority)
        }
    }
}

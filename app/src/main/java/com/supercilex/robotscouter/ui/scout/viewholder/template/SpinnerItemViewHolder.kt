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
    private val mItemEditText: EditText = itemView.findViewById(R.id.name) as EditText

    private lateinit var mPrevText: String
    private lateinit var mSnapshot: DataSnapshot

    fun bind(text: String, snapshot: DataSnapshot) {
        mPrevText = text
        mSnapshot = snapshot

        mItemEditText.setText(text)
        mItemEditText.onFocusChangeListener = this
    }

    override fun requestFocus() {
        mItemEditText.requestFocus()
    }

    override fun onFocusChange(v: View, hasFocus: Boolean) {
        val text = mItemEditText.text.toString()
        if (!hasFocus && !TextUtils.equals(text, mPrevText)) {
            mSnapshot.ref.setValue(text, mSnapshot.priority)
        }
    }
}

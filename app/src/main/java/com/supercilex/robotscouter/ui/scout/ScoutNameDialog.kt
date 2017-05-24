package com.supercilex.robotscouter.ui.scout

import android.support.v4.app.FragmentManager
import android.support.v7.app.AlertDialog
import android.text.InputType
import android.text.TextUtils
import com.google.firebase.database.DatabaseReference
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.util.DatabaseHelper
import com.supercilex.robotscouter.util.show

class ScoutNameDialog : ScoutValueDialogBase<String>() {
    override val value: String? get() {
        val name: String = lastEditText.text.toString()
        return when {
            TextUtils.isEmpty(name) -> null
            else -> name
        }
    }
    override val title: Int = R.string.edit_scout_name
    override val hint: Int = R.string.scout_name

    override fun onShow(dialog: AlertDialog) {
        super.onShow(dialog)
        lastEditText.inputType = InputType.TYPE_TEXT_FLAG_CAP_WORDS
    }

    companion object {
        private val TAG = "ScoutNameDialog"

        fun show(manager: FragmentManager, ref: DatabaseReference, currentValue: String) =
                ScoutNameDialog().show(manager, TAG, DatabaseHelper.getRefBundle(ref)) {
                    putString(CURRENT_VALUE, currentValue)
                }
    }
}

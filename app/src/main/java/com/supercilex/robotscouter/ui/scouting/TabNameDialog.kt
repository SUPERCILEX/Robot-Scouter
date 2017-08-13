package com.supercilex.robotscouter.ui.scouting

import android.content.DialogInterface
import android.support.annotation.StringRes
import android.support.v4.app.FragmentManager
import android.text.InputType
import android.text.TextUtils
import com.google.firebase.database.DatabaseReference
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.util.data.getRefBundle
import com.supercilex.robotscouter.util.ui.show

class TabNameDialog : ValueDialogBase<String>() {
    override val value: String? get() {
        val name: String = lastEditText.text.toString()
        return when {
            TextUtils.isEmpty(name) -> null
            else -> name
        }
    }
    override val title by lazy { arguments.getInt(TITLE_KEY) }
    override val hint = R.string.scout_name

    override fun onShow(dialog: DialogInterface) {
        super.onShow(dialog)
        lastEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
    }

    companion object {
        private const val TAG = "TabNameDialog"
        private const val TITLE_KEY = "title_key"

        fun show(manager: FragmentManager,
                 ref: DatabaseReference,
                 @StringRes title: Int,
                 currentValue: String) = TabNameDialog().show(manager, TAG, getRefBundle(ref)) {
            putInt(TITLE_KEY, title)
            putString(CURRENT_VALUE, currentValue)
        }
    }
}

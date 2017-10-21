package com.supercilex.robotscouter.ui.scouting

import android.content.DialogInterface
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v4.app.FragmentManager
import android.text.InputType
import android.text.TextUtils
import com.google.firebase.firestore.DocumentReference
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.util.FIRESTORE_NAME
import com.supercilex.robotscouter.util.data.getRef
import com.supercilex.robotscouter.util.data.putRef
import com.supercilex.robotscouter.util.ui.show
import com.supercilex.robotscouter.util.unsafeLazy

class TabNameDialog : ValueDialogBase<String>() {
    override val value: String? get() {
        val name: String = lastEditText.text.toString()
        return when {
            TextUtils.isEmpty(name) -> null
            else -> name
        }
    }
    override val title by unsafeLazy { arguments.getInt(TITLE_KEY) }
    override val hint = R.string.scout_name

    override fun onShow(dialog: DialogInterface, savedInstanceState: Bundle?) {
        super.onShow(dialog, savedInstanceState)
        lastEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
    }

    override fun onAttemptDismiss(): Boolean {
        arguments.getRef().update(FIRESTORE_NAME, value)
        return true
    }

    companion object {
        private const val TAG = "TabNameDialog"
        private const val TITLE_KEY = "title_key"

        fun show(manager: FragmentManager,
                 ref: DocumentReference,
                 @StringRes title: Int,
                 currentValue: String) = TabNameDialog().show(manager, TAG) {
            putRef(ref)
            putInt(TITLE_KEY, title)
            putString(CURRENT_VALUE, currentValue)
        }
    }
}

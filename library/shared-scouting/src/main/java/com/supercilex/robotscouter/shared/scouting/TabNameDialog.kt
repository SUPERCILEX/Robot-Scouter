package com.supercilex.robotscouter.shared.scouting

import android.content.DialogInterface
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v4.app.FragmentManager
import android.text.InputType
import com.google.firebase.firestore.DocumentReference
import com.supercilex.robotscouter.common.FIRESTORE_NAME
import com.supercilex.robotscouter.core.data.getRef
import com.supercilex.robotscouter.core.data.nullOrFull
import com.supercilex.robotscouter.core.data.putRef
import com.supercilex.robotscouter.core.logFailures
import com.supercilex.robotscouter.core.ui.show
import com.supercilex.robotscouter.core.unsafeLazy

internal class TabNameDialog : ValueDialogBase<String>() {
    override val value get() = lastEditText.text.nullOrFull()?.toString()
    override val title by unsafeLazy { checkNotNull(arguments).getInt(TITLE_KEY) }
    override val hint = R.string.scout_name_title

    override fun onShow(dialog: DialogInterface, savedInstanceState: Bundle?) {
        super.onShow(dialog, savedInstanceState)
        lastEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
    }

    override fun onAttemptDismiss(): Boolean {
        val ref = checkNotNull(arguments).getRef()
        ref.update(FIRESTORE_NAME, value).logFailures(ref, value)
        return true
    }

    companion object {
        private const val TAG = "TabNameDialog"
        private const val TITLE_KEY = "title_key"

        fun show(
                manager: FragmentManager,
                ref: DocumentReference,
                @StringRes title: Int,
                currentValue: String
        ) = TabNameDialog().show(manager, TAG) {
            putRef(ref)
            putInt(TITLE_KEY, title)
            putString(CURRENT_VALUE, currentValue)
        }
    }
}

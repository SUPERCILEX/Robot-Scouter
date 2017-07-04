package com.supercilex.robotscouter.ui.scout.viewholder

import android.content.DialogInterface
import android.support.v4.app.FragmentManager
import android.text.InputType
import com.google.firebase.database.DatabaseReference
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.ui.scout.ScoutValueDialogBase
import com.supercilex.robotscouter.util.DatabaseHelper
import com.supercilex.robotscouter.util.isNumber
import com.supercilex.robotscouter.util.show

class ScoutCounterValueDialog : ScoutValueDialogBase<Long>() {
    override val value get() = lastEditText.text.toString().toLong()
    override val title = R.string.edit_value
    override val hint = R.string.value

    override fun onShow(dialog: DialogInterface) {
        super.onShow(dialog)
        lastEditText.inputType = InputType.TYPE_CLASS_NUMBER
    }

    override fun onAttemptDismiss() = if (lastEditText.text.toString().isNumber()) {
        super.onAttemptDismiss()
    } else {
        inputLayout.error = getString(R.string.number_too_big_error); false
    }

    companion object {
        private const val TAG = "ScoutCounterValueDialog"

        fun show(manager: FragmentManager, ref: DatabaseReference, currentValue: String) =
                ScoutCounterValueDialog().show(manager, TAG, DatabaseHelper.getRefBundle(ref)) {
                    putString(CURRENT_VALUE, currentValue)
                }
    }
}

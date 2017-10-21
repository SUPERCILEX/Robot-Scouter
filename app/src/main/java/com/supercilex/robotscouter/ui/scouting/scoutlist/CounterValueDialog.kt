package com.supercilex.robotscouter.ui.scouting.scoutlist

import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.text.InputType
import com.google.firebase.firestore.DocumentReference
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.ui.scouting.ValueDialogBase
import com.supercilex.robotscouter.util.FIRESTORE_VALUE
import com.supercilex.robotscouter.util.data.getRef
import com.supercilex.robotscouter.util.data.putRef
import com.supercilex.robotscouter.util.isNumber
import com.supercilex.robotscouter.util.ui.show

class CounterValueDialog : ValueDialogBase<Long>() {
    override val value get() = lastEditText.text.toString().toLong()
    override val title = R.string.edit_value
    override val hint = R.string.value

    override fun onShow(dialog: DialogInterface, savedInstanceState: Bundle?) {
        super.onShow(dialog, savedInstanceState)
        lastEditText.inputType = InputType.TYPE_CLASS_NUMBER
    }

    override fun onAttemptDismiss() = if (lastEditText.text.toString().isNumber()) {
        arguments.getRef().update(FIRESTORE_VALUE, value); true
    } else {
        inputLayout.error = getString(R.string.number_too_big_error); false
    }

    companion object {
        private const val TAG = "CounterValueDialog"

        fun show(manager: FragmentManager, ref: DocumentReference, currentValue: String) =
                CounterValueDialog().show(manager, TAG) {
                    putRef(ref)
                    putString(CURRENT_VALUE, currentValue)
                }
    }
}

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
import com.supercilex.robotscouter.util.logFailures
import com.supercilex.robotscouter.util.ui.show
import kotlinx.android.synthetic.main.dialog_value.*

class CounterValueDialog : ValueDialogBase<Long>() {
    override val value get() = lastEditText.text.toString().toLong()
    override val title = R.string.scout_edit_counter_value_title
    override val hint = R.string.scout_counter_value_title

    override fun onShow(dialog: DialogInterface, savedInstanceState: Bundle?) {
        super.onShow(dialog, savedInstanceState)
        lastEditText.inputType = InputType.TYPE_CLASS_NUMBER
    }

    override fun onAttemptDismiss(): Boolean {
        val number = try {
            value
        } catch (e: NumberFormatException) {
            valueLayout.error = getString(R.string.number_too_big_error)
            return false
        }
        val ref = arguments!!.getRef()
        ref.update(FIRESTORE_VALUE, number).logFailures(ref, number)
        return true
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

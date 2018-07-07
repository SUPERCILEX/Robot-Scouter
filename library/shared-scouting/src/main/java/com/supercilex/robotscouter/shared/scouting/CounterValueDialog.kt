package com.supercilex.robotscouter.shared.scouting

import android.content.DialogInterface
import android.os.Bundle
import android.text.InputType
import androidx.fragment.app.FragmentManager
import com.google.firebase.firestore.DocumentReference
import com.supercilex.robotscouter.common.FIRESTORE_VALUE
import com.supercilex.robotscouter.core.data.getRef
import com.supercilex.robotscouter.core.data.putRef
import com.supercilex.robotscouter.core.logFailures
import com.supercilex.robotscouter.core.ui.show
import kotlinx.android.synthetic.main.dialog_value.*

internal class CounterValueDialog : ValueDialogBase<Long>() {
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
        val ref = checkNotNull(arguments).getRef()
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

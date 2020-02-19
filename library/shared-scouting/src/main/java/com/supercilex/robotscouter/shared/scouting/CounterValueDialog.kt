package com.supercilex.robotscouter.shared.scouting

import android.text.InputType
import androidx.fragment.app.FragmentManager
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.DocumentReference
import com.supercilex.robotscouter.common.FIRESTORE_VALUE
import com.supercilex.robotscouter.core.data.getRef
import com.supercilex.robotscouter.core.data.logFailures
import com.supercilex.robotscouter.core.data.putRef
import com.supercilex.robotscouter.core.ui.show

internal class CounterValueDialog : ValueDialogBase<Long>() {
    override val value get() = lastEditText.text.toString().toLong()
    override val title = R.string.scout_edit_counter_value_title
    override val hint = R.string.scout_counter_value_title

    override fun onStart() {
        super.onStart()
        lastEditText.inputType = InputType.TYPE_CLASS_NUMBER
    }

    override fun onAttemptDismiss(): Boolean {
        val number = try {
            value
        } catch (e: NumberFormatException) {
            requireDialog().findViewById<TextInputLayout>(R.id.value_layout).error =
                    getString(R.string.number_too_big_error)
            return false
        }
        val ref = requireArguments().getRef()
        ref.update(FIRESTORE_VALUE, number).logFailures("updateCounterValue", ref, number)
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

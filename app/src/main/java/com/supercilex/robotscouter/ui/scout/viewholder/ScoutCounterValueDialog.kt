package com.supercilex.robotscouter.ui.scout.viewholder

import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.support.v7.app.AlertDialog
import android.text.InputType
import com.google.firebase.database.DatabaseReference
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.ui.scout.ScoutValueDialogBase
import com.supercilex.robotscouter.util.DatabaseHelper
import java.math.BigDecimal

class ScoutCounterValueDialog : ScoutValueDialogBase<Int>() {
    override val value: Int get() = Integer.valueOf(lastEditText.text.toString())
    override val title: Int = R.string.edit_value
    override val hint: Int = R.string.value

    override fun onShow(dialog: AlertDialog) {
        super.onShow(dialog)
        lastEditText.inputType = InputType.TYPE_CLASS_NUMBER
    }

    override fun onClick(): Boolean {
        try {
            BigDecimal(lastEditText.text.toString()).intValueExact() // Checking for failure
            return super.onClick()
        } catch (e: NumberFormatException) {
            inputLayout.error = getString(R.string.invalid_team_number)
            return false
        } catch (e: ArithmeticException) {
            inputLayout.error = getString(R.string.invalid_team_number)
            return false
        }
    }

    companion object {
        private val TAG = "ScoutCounterValueDialog"

        fun show(manager: FragmentManager, ref: DatabaseReference, currentValue: String) {
            val dialog = ScoutCounterValueDialog()

            val args: Bundle = DatabaseHelper.getRefBundle(ref)
            args.putString(CURRENT_VALUE, currentValue)
            dialog.arguments = args

            dialog.show(manager, TAG)
        }
    }
}

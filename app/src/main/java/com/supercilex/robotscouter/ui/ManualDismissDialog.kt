package com.supercilex.robotscouter.ui

import android.content.DialogInterface
import android.support.annotation.CallSuper
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.util.create

/**
 * Enables choosing whether or not to dismiss the dialog when the positive button is clicked.
 *
 * **Note:** for this class to work correctly, the dialog must be an [AlertDialog] and set a
 * [DialogInterface.OnShowListener].
 */
abstract class ManualDismissDialog : DialogFragment(), DialogInterface.OnShowListener {
    /** @return true if the dialog should be dismissed, false otherwise */
    protected abstract fun onAttemptDismiss(): Boolean

    protected fun AlertDialog.Builder.createAndSetup() = create { onShow(this) }

    @CallSuper
    override fun onShow(dialog: DialogInterface) {
        dialog as AlertDialog
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { handleOnAttemptDismiss() }
    }

    override fun onDestroy() {
        super.onDestroy()
        RobotScouter.getRefWatcher(activity).watch(this)
    }

    protected fun handleOnAttemptDismiss() {
        if (onAttemptDismiss()) dismiss()
    }
}

package com.supercilex.robotscouter.util.ui

import android.content.DialogInterface
import android.os.Bundle
import android.support.annotation.CallSuper
import android.support.annotation.StringRes
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.support.v7.app.AlertDialog
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import com.supercilex.robotscouter.util.refWatcher

inline fun AlertDialog.Builder.create(crossinline listener: AlertDialog.() -> Unit): AlertDialog =
        create().apply { setOnShowListener { (it as AlertDialog).listener() } }

fun DialogFragment.show(manager: FragmentManager,
                        tag: String,
                        args: Bundle = Bundle(),
                        argsListener: (Bundle.() -> Unit)? = null) {
    arguments = args.apply { argsListener?.invoke(this) }
    show(manager, tag)
}

abstract class DialogFragmentBase : DialogFragment() {
    override fun onDestroy() {
        super.onDestroy()
        refWatcher.watch(this)
    }
}

/**
 * Enables choosing whether or not to dismiss the dialog when the positive button is clicked.
 *
 * **Note:** for this class to work correctly, the dialog must be an [AlertDialog] and set a
 * [DialogInterface.OnShowListener].
 */
abstract class ManualDismissDialog : DialogFragment() {
    /** @return true if the dialog should be dismissed, false otherwise */
    protected abstract fun onAttemptDismiss(): Boolean

    protected fun AlertDialog.Builder.createAndSetup(savedInstanceState: Bundle?) =
            create { onShow(this, savedInstanceState) }

    @CallSuper
    open fun onShow(dialog: DialogInterface, savedInstanceState: Bundle?) {
        dialog as AlertDialog
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { handleOnAttemptDismiss() }
    }

    protected fun handleOnAttemptDismiss() {
        if (onAttemptDismiss()) dismiss()
    }
}

abstract class KeyboardDialogBase : ManualDismissDialog(), TextView.OnEditorActionListener {
    protected abstract val lastEditText: EditText

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // Show keyboard
        dialog.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }

    protected fun createDialog(rootView: View, @StringRes title: Int, savedInstanceState: Bundle?) =
            AlertDialog.Builder(context)
                    .setView(rootView)
                    .setTitle(title)
                    .setPositiveButton(android.R.string.ok, null)
                    .setNegativeButton(android.R.string.cancel, null)
                    .createAndSetup(savedInstanceState)

    override fun onShow(dialog: DialogInterface, savedInstanceState: Bundle?) {
        super.onShow(dialog, savedInstanceState)
        lastEditText.setOnEditorActionListener(this)
    }

    override fun onEditorAction(v: TextView, actionId: Int, event: KeyEvent?): Boolean {
        if (event?.keyCode == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE) {
            handleOnAttemptDismiss()
            return true
        }
        return false
    }
}

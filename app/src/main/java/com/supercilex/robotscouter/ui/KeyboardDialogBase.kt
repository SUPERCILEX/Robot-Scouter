package com.supercilex.robotscouter.ui

import android.os.Bundle
import android.support.annotation.CallSuper
import android.support.annotation.StringRes
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.util.createAndListen

abstract class KeyboardDialogBase : DialogFragment(), View.OnClickListener, TextView.OnEditorActionListener {
    protected abstract val lastEditText: EditText

    protected abstract fun onClick(): Boolean

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // Show keyboard
        dialog.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }

    protected fun createDialog(rootView: View, @StringRes title: Int): AlertDialog =
            AlertDialog.Builder(context)
                    .setView(rootView)
                    .setTitle(title)
                    .setPositiveButton(android.R.string.ok, null)
                    .setNegativeButton(android.R.string.cancel, null)
                    .createAndListen { onShow(this) }

    override fun onCreateView(
            inflater: LayoutInflater?,
            container: ViewGroup?,
            savedInstanceState: Bundle?): View? {
        lastEditText.setOnEditorActionListener(this)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    @CallSuper open fun onShow(dialog: AlertDialog) =
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(this)

    override fun onDestroy() {
        super.onDestroy()
        RobotScouter.getRefWatcher(activity).watch(this)
    }

    override fun onClick(v: View) {
        if (onClick()) dialog.dismiss()
    }

    override fun onEditorAction(v: TextView, actionId: Int, event: KeyEvent?): Boolean {
        if (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE) {
            onClick(lastEditText)
            return true
        }
        return false
    }
}

package com.supercilex.robotscouter.ui

import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v7.app.AlertDialog
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView

abstract class KeyboardDialogBase : ManualDismissDialog(), TextView.OnEditorActionListener {
    protected abstract val lastEditText: EditText

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // Show keyboard
        dialog.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }

    protected fun createDialog(rootView: View, @StringRes title: Int) =
            AlertDialog.Builder(context)
                    .setView(rootView)
                    .setTitle(title)
                    .setPositiveButton(android.R.string.ok, null)
                    .setNegativeButton(android.R.string.cancel, null)
                    .createAndSetup()

    override fun onCreateView(inflater: LayoutInflater?,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        lastEditText.setOnEditorActionListener(this)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onEditorAction(v: TextView, actionId: Int, event: KeyEvent?): Boolean {
        if (event?.keyCode == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE) {
            handleOnAttemptDismiss()
            return true
        }
        return false
    }
}

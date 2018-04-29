package com.supercilex.robotscouter.core.ui

import android.content.Context
import android.view.KeyEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import com.supercilex.robotscouter.core.RobotScouter

val inputMethodManager: InputMethodManager by lazy {
    RobotScouter.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
}

fun Window.setKeyboardModeVisible() {
    setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
}

fun View.showKeyboard() {
    inputMethodManager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

inline fun EditText.setImeOnDoneListener(
        crossinline listener: () -> Unit
) = setOnEditorActionListener { _, actionId, event: KeyEvent? ->
    if (event?.keyCode == KeyEvent.KEYCODE_ENTER) {
        if (event.action == KeyEvent.ACTION_UP) listener()

        // We need to return true even if we didn't handle the event to continue
        // receiving future callbacks.
        true
    } else if (actionId == EditorInfo.IME_ACTION_DONE) {
        listener()
        true
    } else {
        false
    }
}

interface KeyboardShortcutListener {
    /** @return true if the shortcut was handled, false otherwise */
    fun onShortcut(keyCode: Int, event: KeyEvent): Boolean
}

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

abstract class KeyboardShortcutHandler : KeyEvent.Callback {
    private val filteredEvents = mutableMapOf<Long, KeyEvent>()

    override fun onKeyMultiple(keyCode: Int, count: Int, event: KeyEvent): Boolean = false

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        event.also { filteredEvents[it.downTime] = it }
        return false
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        filteredEvents.remove(event.downTime)?.let { onFilteredKeyUp(keyCode, event) }
        return false
    }

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent): Boolean = false

    abstract fun onFilteredKeyUp(keyCode: Int, event: KeyEvent)
}

package com.supercilex.robotscouter.util.ui

import android.content.Context
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import com.supercilex.robotscouter.RobotScouter

val inputMethodManager: InputMethodManager by lazy {
    RobotScouter.INSTANCE.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
}

fun Window.setKeyboardModeVisible() {
    setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
}

fun View.showKeyboard() {
    inputMethodManager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

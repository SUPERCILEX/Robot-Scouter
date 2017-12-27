package com.supercilex.robotscouter.util.ui

import android.view.KeyEvent

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

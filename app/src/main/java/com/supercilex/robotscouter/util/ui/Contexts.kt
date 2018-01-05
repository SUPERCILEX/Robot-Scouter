package com.supercilex.robotscouter.util.ui

import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.KeyEvent
import com.google.firebase.analytics.FirebaseAnalytics
import com.supercilex.robotscouter.util.refWatcher

abstract class ActivityBase : AppCompatActivity() {
    protected open val keyboardShortcutHandler: KeyboardShortcutHandler =
            object : KeyboardShortcutHandler() {
                override fun onFilteredKeyUp(keyCode: Int, event: KeyEvent) = Unit
            }

    override fun onKeyMultiple(keyCode: Int, count: Int, event: KeyEvent): Boolean {
        keyboardShortcutHandler.onKeyMultiple(keyCode, count, event)
        return super.onKeyMultiple(keyCode, count, event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        keyboardShortcutHandler.onKeyDown(keyCode, event)
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        keyboardShortcutHandler.onKeyUp(keyCode, event)
        return super.onKeyUp(keyCode, event)
    }

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent): Boolean {
        keyboardShortcutHandler.onKeyLongPress(keyCode, event)
        return super.onKeyLongPress(keyCode, event)
    }
}

abstract class FragmentBase : Fragment() {
    override fun onResume() {
        super.onResume()
        FirebaseAnalytics.getInstance(context)
                .setCurrentScreen(activity!!, null, javaClass.simpleName)
    }

    override fun onDestroy() {
        super.onDestroy()
        refWatcher.watch(this)
    }
}

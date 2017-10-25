package com.supercilex.robotscouter.util.ui

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.support.v4.app.Fragment
import android.support.v4.app.NavUtils
import android.support.v4.app.TaskStackBuilder
import android.support.v7.app.AppCompatActivity
import android.view.KeyEvent
import com.supercilex.robotscouter.util.data.ACTION_FROM_DEEP_LINK
import com.supercilex.robotscouter.util.refWatcher

fun Intent.addNewDocumentFlags(): Intent {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
    }
    return this
}

fun Activity.handleUpNavigation() {
    if (NavUtils.shouldUpRecreateTask(this, NavUtils.getParentActivityIntent(this)!!)
            || intent.action == ACTION_FROM_DEEP_LINK) {
        TaskStackBuilder.create(this).addParentStack(this).startActivities()
        finish()
    } else {
        NavUtils.navigateUpFromSameTask(this)
    }
}

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
    override fun onDestroy() {
        super.onDestroy()
        refWatcher.watch(this)
    }
}

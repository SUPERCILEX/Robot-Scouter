package com.supercilex.robotscouter.util.ui

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import com.google.firebase.analytics.FirebaseAnalytics
import com.supercilex.robotscouter.util.refWatcher

interface OnActivityResult {
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
}

interface Saveable {
    /**
     * @see [android.support.v7.app.AppCompatActivity.onSaveInstanceState]
     * @see [android.support.v4.app.Fragment.onSaveInstanceState]
     */
    fun onSaveInstanceState(outState: Bundle)
}

abstract class ActivityBase : AppCompatActivity(), OnActivityResult, Saveable {
    protected open val keyboardShortcutHandler: KeyboardShortcutHandler =
            object : KeyboardShortcutHandler() {
                override fun onFilteredKeyUp(keyCode: Int, event: KeyEvent) = Unit
            }
    private var clearFocus: Runnable? = null

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

    @Suppress("RedundantOverride") // Needed to relax visibility
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) =
            super.onActivityResult(requestCode, resultCode, data)

    @Suppress("RedundantOverride") // Needed to relax visibility
    override fun onSaveInstanceState(outState: Bundle) = super.onSaveInstanceState(outState)

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val v: View? = currentFocus
        if (ev.action == MotionEvent.ACTION_DOWN && v is EditText) {
            val outRect = Rect()
            v.getGlobalVisibleRect(outRect)
            if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                clearFocus = Runnable {
                    if (currentFocus === v || currentFocus !is EditText) {
                        v.clearFocus()
                        inputMethodManager.hideSoftInputFromWindow(v.windowToken, 0)
                    }
                    clearFocus = null
                }.also {
                    v.postDelayed(it, shortAnimationDuration)
                }
            }
        } else if (ev.action == MotionEvent.ACTION_MOVE && clearFocus != null
                && ev.eventTime - ev.downTime > shortAnimationDuration / 2) {
            v?.removeCallbacks(clearFocus)
        }
        return super.dispatchTouchEvent(ev)
    }
}

abstract class FragmentBase : Fragment(), OnActivityResult, Saveable {
    override fun onResume() {
        super.onResume()
        FirebaseAnalytics.getInstance(context)
                .setCurrentScreen(requireActivity(), null, javaClass.simpleName)
    }

    override fun onDestroy() {
        super.onDestroy()
        refWatcher.watch(this)
    }
}

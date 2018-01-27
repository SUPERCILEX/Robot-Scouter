package com.supercilex.robotscouter.util.ui

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import com.google.firebase.analytics.FirebaseAnalytics
import com.supercilex.robotscouter.util.refWatcher

interface OnActivityResult {
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
}

abstract class ActivityBase : AppCompatActivity(), OnActivityResult {
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
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val v: View? = currentFocus
        if (ev.action == MotionEvent.ACTION_DOWN && v is EditText) {
            val outRect = Rect()
            v.getGlobalVisibleRect(outRect)
            if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                clearFocus = Runnable {
                    v.clearFocus()
                    (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                            .hideSoftInputFromWindow(v.windowToken, 0)
                    clearFocus = null
                }.also {
                    v.postDelayed(it, shortAnimationDuration)
                }
            }
        } else if (ev.action == MotionEvent.ACTION_MOVE) {
            v?.removeCallbacks(clearFocus)
        }
        return super.dispatchTouchEvent(ev)
    }
}

abstract class FragmentBase : Fragment(), OnActivityResult {
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

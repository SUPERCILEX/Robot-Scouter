package com.supercilex.robotscouter.core.ui

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.preference.PreferenceFragmentCompat
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import com.google.firebase.analytics.FirebaseAnalytics
import com.supercilex.robotscouter.core.refWatcher

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

abstract class ActivityBase : AppCompatActivity(), OnActivityResult, Saveable,
        KeyboardShortcutListener {
    private val filteredEvents = mutableMapOf<Long, KeyEvent>()
    private var clearFocus: Runnable? = null

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        event.also { filteredEvents[it.downTime] = it }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (filteredEvents.remove(event.downTime) != null) {
            if (onShortcut(keyCode, event)) return true
            val fragmentHandled = supportFragmentManager.fragments
                    .filterIsInstance<KeyboardShortcutListener>()
                    .any { it.onShortcut(keyCode, event) }
            if (fragmentHandled) return true
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onShortcut(keyCode: Int, event: KeyEvent) = false

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
        } else if (
            ev.action == MotionEvent.ACTION_MOVE && clearFocus != null &&
            ev.eventTime - ev.downTime > shortAnimationDuration / 2
        ) {
            v?.removeCallbacks(clearFocus)
        }
        return super.dispatchTouchEvent(ev)
    }
}

abstract class FragmentBase : Fragment(), OnActivityResult, Saveable {
    override fun onResume() {
        super.onResume()
        FirebaseAnalytics.getInstance(requireContext())
                .setCurrentScreen(requireActivity(), null, javaClass.simpleName)
    }

    override fun onDestroy() {
        super.onDestroy()
        refWatcher.watch(this)
    }
}

abstract class PreferenceFragmentBase : PreferenceFragmentCompat(), OnActivityResult, Saveable {
    override fun onResume() {
        super.onResume()
        FirebaseAnalytics.getInstance(requireContext())
                .setCurrentScreen(requireActivity(), null, javaClass.simpleName)
    }

    override fun onDestroy() {
        super.onDestroy()
        refWatcher.watch(this)
    }
}

package com.supercilex.robotscouter.core.ui

import android.os.SystemClock
import android.view.View
import androidx.core.view.isVisible

internal interface ContentLoader {
    val helper: ContentLoaderHelper

    /**
     * Show the progress view after waiting for a minimum delay. If during that time, hide() is
     * called, the view is never made visible.
     */
    fun show(callback: Runnable? = null) = helper.show(callback)

    /**
     * Hide the progress view if it is visible. The progress view will not be hidden until it has
     * been shown for at least a minimum show time. If the progress view was not yet visible,
     * cancels showing the progress view.
     */
    fun hide(force: Boolean = false, callback: Runnable? = null) = helper.hide(force, callback)
}

class ContentLoaderHelper(private val view: View, show: () -> Unit, hide: () -> Unit) {
    private val delayedShow = Runnable {
        startTime = SystemClock.uptimeMillis()
        show()
    }
    private val delayedHide = Runnable {
        view.isVisible = false
        hide()
    }

    private var isAttachedToWindow = false
    private var isShown: Boolean = view.isVisible
    private var startTime = -1L

    fun onAttachedToWindow() {
        isAttachedToWindow = true

        if (isShown && !view.isVisible) view.postDelayed(delayedShow, MIN_DELAY_MILLIS)
    }

    fun onDetachedFromWindow() {
        isAttachedToWindow = false

        view.removeCallbacks(delayedHide)
        view.removeCallbacks(delayedShow)

        if (!isShown && startTime != -1L) view.isVisible = false
        startTime = -1L
    }

    fun show(callback: Runnable?) {
        if (!isShown) {
            isShown = true
            if (isAttachedToWindow) {
                view.removeCallbacks(delayedHide)
                if (startTime == -1L) postDelayeds(MIN_DELAY_MILLIS, delayedShow, callback)
            } else {
                callback?.run()
            }
        }
    }

    fun hide(force: Boolean, callback: Runnable?) {
        if (isShown) {
            isShown = false
            if (isAttachedToWindow) view.removeCallbacks(delayedShow)

            val diff = SystemClock.uptimeMillis() - startTime
            if (startTime == -1L || diff >= MIN_SHOW_TIME_MILLIS || force) {
                // The progress spinner has been shown long enough OR was not shown yet.
                // If it wasn't shown yet, it will just never be shown.
                view.isVisible = false
                callback?.run()
                startTime = -1L
            } else {
                // The progress spinner is shown, but not long enough,
                // so put a delayed message in to hide it when its been shown long enough.
                postDelayeds(MIN_SHOW_TIME_MILLIS - diff, delayedHide, callback)
            }
        }
    }

    private fun postDelayeds(delayMillis: Long, vararg actions: Runnable?) =
            actions.filterNotNull().forEach { view.postDelayed(it, delayMillis) }

    private companion object {
        const val MIN_SHOW_TIME_MILLIS = 500L
        const val MIN_DELAY_MILLIS = 500L
    }
}

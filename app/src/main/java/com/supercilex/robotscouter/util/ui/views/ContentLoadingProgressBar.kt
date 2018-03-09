package com.supercilex.robotscouter.util.ui.views

import android.content.Context
import android.os.SystemClock
import android.util.AttributeSet
import android.widget.ProgressBar
import androidx.view.isVisible

/**
 * ContentLoadingProgressBar implements a ProgressBar that waits a minimum time to be dismissed
 * before showing. Once visible, the progress bar will be visible for a minimum amount of time to
 * avoid "flashes" in the UI when an event could take a largely variable time to complete
 * (from none, to a user perceivable amount).
 */
class ContentLoadingProgressBar : ProgressBar {
    private val delayedShow = Runnable {
        startTime = SystemClock.uptimeMillis()
        isVisible = true
    }
    private val delayedHide = Runnable {
        isVisible = false
        startTime = -1L
    }

    private var _isAttachedToWindow = false
    private var _isShown: Boolean = isVisible
    private var startTime = -1L

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr)

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        _isAttachedToWindow = true

        if (_isShown && !isVisible) postDelayed(delayedShow, MIN_DELAY_MILLIS)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        _isAttachedToWindow = false

        removeCallbacks(delayedHide)
        removeCallbacks(delayedShow)

        if (!_isShown && startTime != -1L) isVisible = false
        startTime = -1L
    }

    /**
     * Hide the progress view if it is visible. The progress view will not be hidden until it has
     * been shown for at least a minimum show time. If the progress view was not yet visible,
     * cancels showing the progress view.
     */
    fun hide(force: Boolean = false, callback: Runnable? = null) {
        if (_isShown) {
            _isShown = false
            if (_isAttachedToWindow) removeCallbacks(delayedShow)

            val diff = SystemClock.uptimeMillis() - startTime
            if (startTime == -1L || diff >= MIN_SHOW_TIME_MILLIS || force) {
                // The progress spinner has been shown long enough OR was not shown yet.
                // If it wasn't shown yet, it will just never be shown.
                isVisible = false
                callback?.run()
                startTime = -1L
            } else {
                // The progress spinner is shown, but not long enough,
                // so put a delayed message in to hide it when its been shown long enough.
                postDelayeds(MIN_SHOW_TIME_MILLIS - diff, delayedHide, callback)
            }
        }
    }

    /**
     * Show the progress view after waiting for a minimum delay. If during that time, hide() is
     * called, the view is never made visible.
     */
    fun show(callback: Runnable? = null) {
        if (!_isShown) {
            _isShown = true
            if (_isAttachedToWindow) {
                removeCallbacks(delayedHide)
                if (startTime == -1L) postDelayeds(MIN_DELAY_MILLIS, delayedShow, callback)
            } else {
                callback?.run()
            }
        }
    }

    private fun postDelayeds(delayMillis: Long, vararg actions: Runnable?) =
            actions.filterNotNull().forEach { postDelayed(it, delayMillis) }

    private companion object {
        const val MIN_SHOW_TIME_MILLIS = 500L
        const val MIN_DELAY_MILLIS = 500L
    }
}

package com.supercilex.robotscouter.util.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Debug
import android.os.SystemClock
import android.support.annotation.ColorRes
import android.support.design.widget.TextInputEditText
import android.support.design.widget.TextInputLayout
import android.support.text.emoji.widget.EmojiAppCompatEditText
import android.support.v4.content.ContextCompat
import android.support.v4.widget.TextViewCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDelegate
import android.support.v7.content.res.AppCompatResources
import android.support.v7.widget.AppCompatButton
import android.support.v7.widget.AppCompatTextView
import android.util.AttributeSet
import android.view.View
import android.view.ViewAnimationUtils
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.ProgressBar
import android.widget.TextView
import com.supercilex.robotscouter.BuildConfig
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.util.data.ChangeEventListenerBase
import com.supercilex.robotscouter.util.data.PrefsLiveData
import com.supercilex.robotscouter.util.data.nightMode
import java.util.concurrent.CopyOnWriteArrayList

private val visibleActivities: MutableList<Activity> = CopyOnWriteArrayList()

fun initUi() {
    AppCompatDelegate.setDefaultNightMode(nightMode)
    PrefsLiveData.observeForever {
        it?.addChangeEventListener(object : ChangeEventListenerBase {
            override fun onDataChanged() {
                AppCompatDelegate.setDefaultNightMode(nightMode)
                visibleActivities.filterIsInstance<AppCompatActivity>()
                        .forEach { it.delegate.setLocalNightMode(nightMode) }
            }
        })
    }

    RobotScouter.INSTANCE.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            if (BuildConfig.DEBUG) {
                val window = activity.window
                if (Debug.isDebuggerConnected()) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
        }

        override fun onActivityStarted(activity: Activity) {
            visibleActivities += activity
        }

        override fun onActivityResumed(activity: Activity) {
            activity.findViewById<View>(android.R.id.content).post {
                (activity as? AppCompatActivity ?: return@post).delegate.setLocalNightMode(nightMode)
            }
        }

        override fun onActivityPaused(activity: Activity) = Unit

        override fun onActivityStopped(activity: Activity) {
            visibleActivities -= activity
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

        override fun onActivityDestroyed(activity: Activity) = Unit
    })
}

fun isInTabletMode(context: Context): Boolean {
    val config: Configuration = context.resources.configuration
    val size: Int = config.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
    return size == Configuration.SCREENLAYOUT_SIZE_LARGE && config.orientation == Configuration.ORIENTATION_LANDSCAPE
            || size > Configuration.SCREENLAYOUT_SIZE_LARGE
}

fun animateColorChange(@ColorRes from: Int,
                       @ColorRes to: Int,
                       listener: ValueAnimator.AnimatorUpdateListener) {
    ValueAnimator.ofObject(
            ArgbEvaluator(),
            ContextCompat.getColor(RobotScouter.INSTANCE, from),
            ContextCompat.getColor(RobotScouter.INSTANCE, to)).apply {
        addUpdateListener(listener)
        start()
    }
}

fun animateCircularReveal(view: View, visible: Boolean) {
    val centerX: Int = view.width / 2
    val centerY: Int = view.height / 2
    val animator: Animator? = animateCircularReveal(
            view,
            visible,
            centerX,
            centerY,
            Math.hypot(centerX.toDouble(), centerY.toDouble()).toFloat())
    animator?.start()
}

fun animateCircularReveal(view: View,
                          visible: Boolean,
                          centerX: Int,
                          centerY: Int,
                          radius: Float): Animator? {
    if (visible && view.visibility == View.VISIBLE || !visible && view.visibility == View.GONE) {
        return null
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        if (!view.isAttachedToWindow) {
            view.visibility = if (visible) View.VISIBLE else View.GONE
            return null
        }

        val anim: Animator = ViewAnimationUtils.createCircularReveal(
                view,
                centerX,
                centerY,
                if (visible) 0f else radius,
                if (visible) radius else 0f)

        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (!visible) view.visibility = View.GONE
            }
        })
        if (visible) view.visibility = View.VISIBLE

        return anim
    } else {
        view.visibility = if (visible) View.VISIBLE else View.GONE
        return null
    }
}

/**
 * ContentLoadingProgressBar implements a ProgressBar that waits a minimum time to be dismissed
 * before showing. Once visible, the progress bar will be visible for a minimum amount of time to
 * avoid "flashes" in the UI when an event could take a largely variable time to complete
 * (from none, to a user perceivable amount).
 */
class ContentLoadingProgressBar : ProgressBar {
    private val delayedShow = Runnable {
        startTime = SystemClock.uptimeMillis()
        visibility = View.VISIBLE
    }
    private val delayedHide = Runnable {
        visibility = View.GONE
        startTime = -1L
    }

    private var _isAttachedToWindow = false
    private var _isShown: Boolean = visibility == View.VISIBLE
    private var startTime = -1L

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr)

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        _isAttachedToWindow = true

        if (_isShown && visibility != View.VISIBLE) postDelayed(delayedShow, MIN_DELAY)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        _isAttachedToWindow = false

        removeCallbacks(delayedHide)
        removeCallbacks(delayedShow)

        if (!_isShown && startTime != -1L) visibility = View.GONE
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
            if (startTime == -1L || diff >= MIN_SHOW_TIME || force) {
                // The progress spinner has been shown long enough OR was not shown yet.
                // If it wasn't shown yet, it will just never be shown.
                visibility = View.GONE
                callback?.run()
                startTime = -1L
            } else {
                // The progress spinner is shown, but not long enough,
                // so put a delayed message in to hide it when its been shown long enough.
                postDelayeds(MIN_SHOW_TIME - diff, delayedHide, callback)
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
                if (startTime == -1L) postDelayeds(MIN_DELAY, delayedShow, callback)
            } else {
                callback?.run()
            }
        }
    }

    private fun postDelayeds(delayMillis: Long, vararg actions: Runnable?) =
            actions.filterNotNull().forEach { postDelayed(it, delayMillis) }

    private companion object {
        const val MIN_SHOW_TIME = 500L // ms
        const val MIN_DELAY = 500L // ms
    }
}

/**
 * Wrapper emoji compat class to support showing hint when phone is in landscape mode i.e. when IME
 * is in 'extract' mode.
 */
class EmojiCompatTextInputEditText : EmojiAppCompatEditText {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr)

    /** Copied from [TextInputEditText] */
    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val ic = super.onCreateInputConnection(outAttrs)
        if (ic != null && outAttrs.hintText == null) {
            // If we don't have a hint and our parent is a TextInputLayout, use it's hint for the
            // EditorInfo. This allows us to display a hint in 'extract mode'.
            var parent = parent
            while (parent is View) {
                if (parent is TextInputLayout) {
                    outAttrs.hintText = parent.hint
                    break
                }
                parent = parent.getParent()
            }
        }
        return ic
    }
}

class SupportVectorDrawablesTextView : AppCompatTextView {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initSupportVectorDrawablesAttrs(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr) {
        initSupportVectorDrawablesAttrs(attrs)
    }
}

class SupportVectorDrawablesButton : AppCompatButton {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initSupportVectorDrawablesAttrs(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr) {
        initSupportVectorDrawablesAttrs(attrs)
    }
}

private fun TextView.initSupportVectorDrawablesAttrs(attrs: AttributeSet?) {
    if (attrs == null) return

    val attributeArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.SupportVectorDrawablesTextView)

    var drawableStart: Drawable? = null
    var drawableEnd: Drawable? = null
    var drawableBottom: Drawable? = null
    var drawableTop: Drawable? = null

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        drawableStart = attributeArray.getDrawable(
                R.styleable.SupportVectorDrawablesTextView_drawableStartCompat)
        drawableEnd = attributeArray.getDrawable(
                R.styleable.SupportVectorDrawablesTextView_drawableEndCompat)
        drawableBottom = attributeArray.getDrawable(
                R.styleable.SupportVectorDrawablesTextView_drawableBottomCompat)
        drawableTop = attributeArray.getDrawable(
                R.styleable.SupportVectorDrawablesTextView_drawableTopCompat)
    } else {
        val drawableStartId = attributeArray.getResourceId(
                R.styleable.SupportVectorDrawablesTextView_drawableStartCompat, -1)
        val drawableEndId = attributeArray.getResourceId(
                R.styleable.SupportVectorDrawablesTextView_drawableEndCompat, -1)
        val drawableBottomId = attributeArray.getResourceId(
                R.styleable.SupportVectorDrawablesTextView_drawableBottomCompat, -1)
        val drawableTopId = attributeArray.getResourceId(
                R.styleable.SupportVectorDrawablesTextView_drawableTopCompat, -1)

        if (drawableStartId != -1) {
            drawableStart = AppCompatResources.getDrawable(context, drawableStartId)
        }
        if (drawableEndId != -1) {
            drawableEnd = AppCompatResources.getDrawable(context, drawableEndId)
        }
        if (drawableBottomId != -1) {
            drawableBottom = AppCompatResources.getDrawable(context, drawableBottomId)
        }
        if (drawableTopId != -1) {
            drawableTop = AppCompatResources.getDrawable(context, drawableTopId)
        }
    }

    TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(
            this, drawableStart, drawableTop, drawableEnd, drawableBottom)

    attributeArray.recycle()
}

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
import android.support.annotation.ColorRes
import android.support.v4.content.ContextCompat
import android.support.v4.widget.TextViewCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDelegate
import android.support.v7.content.res.AppCompatResources
import android.util.AttributeSet
import android.view.View
import android.view.ViewAnimationUtils
import android.view.WindowManager
import android.widget.TextView
import com.supercilex.robotscouter.BuildConfig
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.util.data.ChangeEventListenerBase
import com.supercilex.robotscouter.util.data.PrefsLiveData
import com.supercilex.robotscouter.util.data.nightMode
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.hypot

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
            hypot(centerX.toDouble(), centerY.toDouble()).toFloat())
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

internal fun TextView.initSupportVectorDrawablesAttrs(attrs: AttributeSet?) {
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

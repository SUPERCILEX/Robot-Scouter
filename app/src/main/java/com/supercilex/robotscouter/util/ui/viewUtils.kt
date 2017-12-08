package com.supercilex.robotscouter.util.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ArgbEvaluator
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.app.Activity
import android.app.Application
import android.arch.lifecycle.DefaultLifecycleObserver
import android.arch.lifecycle.LifecycleOwner
import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Debug
import android.support.annotation.ColorRes
import android.support.text.emoji.EmojiCompat
import android.support.text.emoji.FontRequestEmojiCompatConfig
import android.support.v4.content.ContextCompat
import android.support.v4.provider.FontRequest
import android.support.v4.view.ViewCompat
import android.support.v4.view.animation.FastOutLinearInInterpolator
import android.support.v4.view.animation.LinearOutSlowInInterpolator
import android.support.v4.widget.TextViewCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDelegate
import android.support.v7.content.res.AppCompatResources
import android.util.AttributeSet
import android.view.View
import android.view.ViewAnimationUtils
import android.view.WindowManager
import android.widget.TextView
import com.crashlytics.android.Crashlytics
import com.google.firebase.crash.FirebaseCrash
import com.supercilex.robotscouter.BuildConfig
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.util.data.ChangeEventListenerBase
import com.supercilex.robotscouter.util.data.ListenerRegistrationLifecycleOwner
import com.supercilex.robotscouter.util.data.PrefsLiveData
import com.supercilex.robotscouter.util.data.nightMode
import org.jetbrains.anko.configuration
import org.jetbrains.anko.find
import org.jetbrains.anko.landscape
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.hypot

val shortAnimationDuration: Long by lazy {
    RobotScouter.INSTANCE.resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
}

private val visibleActivities: MutableList<Activity> = CopyOnWriteArrayList()

fun initUi() {
    AppCompatDelegate.setDefaultNightMode(nightMode)
    PrefsLiveData.observeForever {
        val lifecycle = ListenerRegistrationLifecycleOwner.lifecycle
        if (it == null) {
            lifecycle.removeObserver(NightModeUpdateReceiver)
            NightModeUpdateReceiver.onStop(ListenerRegistrationLifecycleOwner)
        } else {
            lifecycle.addObserver(NightModeUpdateReceiver)
        }
    }

    RobotScouter.INSTANCE.registerActivityLifecycleCallbacks(ActivityHandler)

    EmojiCompat.init(FontRequestEmojiCompatConfig(
            RobotScouter.INSTANCE,
            FontRequest(
                    "com.google.android.gms.fonts",
                    "com.google.android.gms",
                    "Noto Color Emoji Compat",
                    R.array.com_google_android_gms_fonts_certs)).registerInitCallback(
            object : EmojiCompat.InitCallback() {
                override fun onFailed(t: Throwable?) {
                    FirebaseCrash.log("EmojiCompat failed to initialize with error: $t")
                    Crashlytics.log("EmojiCompat failed to initialize with error: $t")
                }
            }))
}

fun isInTabletMode(context: Context): Boolean {
    val config: Configuration = context.configuration
    val size: Int = config.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
    return size == Configuration.SCREENLAYOUT_SIZE_LARGE && config.landscape
            || size > Configuration.SCREENLAYOUT_SIZE_LARGE
}

fun animateColorChange(
        @ColorRes from: Int,
        @ColorRes to: Int,
        listener: ValueAnimator.AnimatorUpdateListener
) {
    ValueAnimator.ofObject(
            ArgbEvaluator(),
            ContextCompat.getColor(RobotScouter.INSTANCE, from),
            ContextCompat.getColor(RobotScouter.INSTANCE, to)).apply {
        addUpdateListener(listener)
        start()
    }
}

fun View.animateCircularReveal(visible: Boolean) {
    val centerX: Int = width / 2
    val centerY: Int = height / 2
    val animator: Animator? = animateCircularReveal(
            visible,
            centerX,
            centerY,
            hypot(centerX.toDouble(), centerY.toDouble()).toFloat()
    )
    animator?.start()
}

fun View.animateCircularReveal(
        visible: Boolean,
        centerX: Int,
        centerY: Int,
        radius: Float
): Animator? = getRevealAnimation(visible) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
        visibility = if (visible) View.VISIBLE else View.GONE
        return@getRevealAnimation null
    }

    val anim: Animator = ViewAnimationUtils.createCircularReveal(
            this,
            centerX,
            centerY,
            if (visible) 0f else radius,
            if (visible) radius else 0f
    )

    anim.addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationStart(animation: Animator?) {
            if (visible) visibility = View.VISIBLE
        }

        override fun onAnimationEnd(animation: Animator) {
            if (!visible) visibility = View.GONE
        }
    })

    anim
}

fun View.animatePopReveal(visible: Boolean) {
    getRevealAnimation(visible) {
        if (visible) {
            alpha = 0f
            scaleY = 0f
            scaleX = 0f
            visibility = View.VISIBLE
        }

        animate().cancel()
        animate()
                .scaleX(if (visible) 1f else 0f)
                .scaleY(if (visible) 1f else 0f)
                .alpha(if (visible) 1f else 0f)
                .setDuration(shortAnimationDuration)
                // Sadly, LookupTableInterpolator is package private in Java which makes Kotlin
                // throw a IllegalAccessError. See https://youtrack.jetbrains.com/issue/KT-15315.
                .setInterpolator(@Suppress("USELESS_CAST") if (visible) {
                    LinearOutSlowInInterpolator() as Any
                } else {
                    FastOutLinearInInterpolator() as Any
                } as TimeInterpolator)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationCancel(animation: Animator) {
                        visibility = if (visible) View.GONE else View.VISIBLE
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        if (!visible) visibility = View.GONE
                        // Reset state
                        alpha = 1f
                        scaleY = 1f
                        scaleX = 1f
                    }
                })
    }
}

fun TextView.initSupportVectorDrawablesAttrs(attrs: AttributeSet?) {
    if (attrs == null) return

    val attributeArray =
            context.obtainStyledAttributes(attrs, R.styleable.SupportVectorDrawablesTextView)

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

private inline fun <T> View.getRevealAnimation(visible: Boolean, animator: () -> T?): T? {
    return if (visible && visibility == View.VISIBLE || !visible && visibility != View.VISIBLE) {
        null
    } else if (!ViewCompat.isAttachedToWindow(this)) {
        visibility = if (visible) View.VISIBLE else View.GONE
        null
    } else {
        animator()
    }
}

private object NightModeUpdateReceiver : DefaultLifecycleObserver, ChangeEventListenerBase {
    override fun onStart(owner: LifecycleOwner) {
        PrefsLiveData.value?.addChangeEventListener(this)
    }

    override fun onStop(owner: LifecycleOwner) {
        PrefsLiveData.value?.removeChangeEventListener(this)
    }

    override fun onDataChanged() {
        AppCompatDelegate.setDefaultNightMode(nightMode)
        visibleActivities.filterIsInstance<AppCompatActivity>()
                .forEach { it.delegate.setLocalNightMode(nightMode) }
    }
}

private object ActivityHandler : Application.ActivityLifecycleCallbacks {
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
        activity.find<View>(android.R.id.content).post {
            (activity as? AppCompatActivity)?.delegate?.setLocalNightMode(nightMode)
        }
    }

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) {
        visibleActivities -= activity
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit
}

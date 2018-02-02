package com.supercilex.robotscouter.util.ui

import android.app.Activity
import android.app.Application
import android.arch.lifecycle.DefaultLifecycleObserver
import android.arch.lifecycle.LifecycleOwner
import android.content.Context
import android.content.res.Configuration
import android.content.res.TypedArray
import android.os.Build
import android.os.Bundle
import android.os.Debug
import android.support.text.emoji.EmojiCompat
import android.support.text.emoji.FontRequestEmojiCompatConfig
import android.support.v4.provider.FontRequest
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDelegate
import android.util.AttributeSet
import android.view.View
import android.view.WindowManager
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

    RobotScouter.registerActivityLifecycleCallbacks(ActivityHandler)

    EmojiCompat.init(FontRequestEmojiCompatConfig(
            RobotScouter,
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

fun Context.isInTabletMode(): Boolean {
    val size: Int = configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
    return size == Configuration.SCREENLAYOUT_SIZE_LARGE && configuration.landscape
            || size > Configuration.SCREENLAYOUT_SIZE_LARGE
}

inline fun <T> Context.obtainStyledAttributes(
        set: AttributeSet,
        attrs: IntArray,
        block: TypedArray.() -> T
): T {
    val a = obtainStyledAttributes(set, attrs)
    return a.block().also { a.recycle() }
}

fun View.setOnLongClickListenerCompat(listener: View.OnLongClickListener) {
    setOnLongClickListener(listener)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        setOnContextClickListener { listener.onLongClick(this) }
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

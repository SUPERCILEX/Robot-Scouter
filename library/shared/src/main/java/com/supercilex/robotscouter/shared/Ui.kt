package com.supercilex.robotscouter.shared

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Debug
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NavUtils
import androidx.core.app.TaskStackBuilder
import androidx.core.provider.FontRequest
import androidx.emoji.text.EmojiCompat
import androidx.emoji.text.FontRequestEmojiCompatConfig
import androidx.lifecycle.Lifecycle
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.await
import com.supercilex.robotscouter.core.data.ACTION_FROM_DEEP_LINK
import com.supercilex.robotscouter.core.data.ChangeEventListenerBase
import com.supercilex.robotscouter.core.data.activitiesRegistry
import com.supercilex.robotscouter.core.data.nightMode
import com.supercilex.robotscouter.core.data.prefs
import com.supercilex.robotscouter.core.logCrashLog
import com.supercilex.robotscouter.core.logFailures
import com.supercilex.robotscouter.shared.client.onSignedIn
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.async
import org.jetbrains.anko.find
import org.jetbrains.anko.longToast
import org.jetbrains.anko.runOnUiThread
import java.util.concurrent.CopyOnWriteArrayList

private val visibleActivities = CopyOnWriteArrayList<Activity>()

fun initUi() {
    GlobalScope.async {
        // Disk I/O sometimes occurs in these
        AuthUI.getInstance()
        GoogleSignIn.getLastSignedInAccount(RobotScouter)

        onSignedIn()
    }.logFailures()

    FirebaseAuth.getInstance().addAuthStateListener {
        it.currentUser?.reload()?.addOnFailureListener {
            if (it is FirebaseException) {
                logCrashLog("User refresh error: $it")

                // If we got a user not found error, it means we've deleted the user record and
                // all associated data, but the user was still using Robot Scouter somehow.
                if (it.message?.contains("USER_NOT_FOUND") == true) {
                    GlobalScope.async {
                        AuthUI.getInstance().signOut(RobotScouter).await()
                        onSignedIn()
                        RobotScouter.runOnUiThread {
                            longToast("User account deleted due to inactivity. " +
                                              "Starting a fresh session.")
                        }
                    }.logFailures()
                }
            }
        }
    }

    activitiesRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

    AppCompatDelegate.setDefaultNightMode(nightMode)
    prefs.addChangeEventListener(object : ChangeEventListenerBase {
        override fun onDataChanged() {
            AppCompatDelegate.setDefaultNightMode(nightMode)
            visibleActivities.filterIsInstance<AppCompatActivity>()
                    .forEach { it.delegate.setLocalNightMode(nightMode) }
        }
    })

    (RobotScouter as Application).registerActivityLifecycleCallbacks(ActivityHandler)

    EmojiCompat.init(FontRequestEmojiCompatConfig(
            RobotScouter,
            FontRequest(
                    "com.google.android.gms.fonts",
                    "com.google.android.gms",
                    "Noto Color Emoji Compat",
                    R.array.com_google_android_gms_fonts_certs)).registerInitCallback(
            object : EmojiCompat.InitCallback() {
                override fun onFailed(t: Throwable?) =
                        logCrashLog("EmojiCompat failed to initialize with error: $t")
            }))
}

fun Activity.handleUpNavigation() = if (
    NavUtils.shouldUpRecreateTask(this, checkNotNull(NavUtils.getParentActivityIntent(this))) ||
    intent.action == ACTION_FROM_DEEP_LINK
) {
    TaskStackBuilder.create(this).addParentStack(this).startActivities()
    finish()
} else {
    NavUtils.navigateUpFromSameTask(this)
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
        if (visibleActivities.isEmpty()) {
            activitiesRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        }
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
        if (visibleActivities.isEmpty()) {
            activitiesRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit
}

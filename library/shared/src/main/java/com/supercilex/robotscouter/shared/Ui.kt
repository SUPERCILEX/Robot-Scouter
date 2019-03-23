package com.supercilex.robotscouter.shared

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NavUtils
import androidx.core.app.TaskStackBuilder
import androidx.core.provider.FontRequest
import androidx.emoji.text.EmojiCompat
import androidx.emoji.text.FontRequestEmojiCompatConfig
import androidx.lifecycle.Lifecycle
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.data.ACTION_FROM_DEEP_LINK
import com.supercilex.robotscouter.core.data.ChangeEventListenerBase
import com.supercilex.robotscouter.core.data.activitiesRegistry
import com.supercilex.robotscouter.core.data.cleanup
import com.supercilex.robotscouter.core.data.nightMode
import com.supercilex.robotscouter.core.data.prefs
import com.supercilex.robotscouter.core.logBreadcrumb
import com.supercilex.robotscouter.shared.client.idpSignOut
import com.supercilex.robotscouter.shared.client.onSignedIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.longToast
import java.util.concurrent.CopyOnWriteArrayList

private val visibleActivities = CopyOnWriteArrayList<Activity>()

fun initUi() {
    GlobalScope.launch {
        // Disk I/O sometimes occurs in these
        AuthUI.getInstance()
        GoogleSignIn.getLastSignedInAccount(RobotScouter)

        onSignedIn()
    }

    FirebaseAuth.getInstance().addAuthStateListener {
        it.currentUser?.reload()?.addOnFailureListener f@{
            if (
                it !is FirebaseAuthInvalidUserException ||
                it.errorCode != "ERROR_USER_NOT_FOUND" && it.errorCode != "ERROR_USER_DISABLED"
            ) return@f

            logBreadcrumb("User deleted or disabled. Re-initializing Robot Scouter.")
            GlobalScope.launch {
                cleanup()
                idpSignOut()
                onSignedIn()
                withContext(Dispatchers.Main) {
                    RobotScouter.longToast(
                            "User account deleted due to inactivity. Starting a fresh session.")
                }
            }
        }
    }

    activitiesRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

    AppCompatDelegate.setDefaultNightMode(nightMode)
    prefs.addChangeEventListener(object : ChangeEventListenerBase {
        override fun onDataChanged() {
            AppCompatDelegate.setDefaultNightMode(nightMode)
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
                        logBreadcrumb("EmojiCompat failed to initialize with error: $t")
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
    override fun onActivityStarted(activity: Activity) {
        if (visibleActivities.isEmpty()) {
            activitiesRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        }
        visibleActivities += activity
    }

    override fun onActivityStopped(activity: Activity) {
        visibleActivities -= activity
        if (visibleActivities.isEmpty()) {
            activitiesRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityResumed(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit
}

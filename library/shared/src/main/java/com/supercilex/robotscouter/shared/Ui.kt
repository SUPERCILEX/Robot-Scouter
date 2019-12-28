package com.supercilex.robotscouter.shared

import android.app.Activity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NavUtils
import androidx.core.app.TaskStackBuilder
import androidx.core.provider.FontRequest
import androidx.emoji.text.EmojiCompat
import androidx.emoji.text.FontRequestEmojiCompatConfig
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.data.ACTION_FROM_DEEP_LINK
import com.supercilex.robotscouter.core.data.ChangeEventListenerBase
import com.supercilex.robotscouter.core.data.cleanup
import com.supercilex.robotscouter.core.data.nightMode
import com.supercilex.robotscouter.core.data.prefs
import com.supercilex.robotscouter.core.logBreadcrumb
import com.supercilex.robotscouter.core.longToast
import com.supercilex.robotscouter.shared.client.idpSignOut
import com.supercilex.robotscouter.shared.client.onSignedIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.invoke
import kotlinx.coroutines.launch

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
                Dispatchers.Main {
                    longToast("User account deleted due to inactivity. Starting a fresh session.")
                }
            }
        }
    }

    AppCompatDelegate.setDefaultNightMode(nightMode)
    prefs.addChangeEventListener(object : ChangeEventListenerBase {
        override fun onDataChanged() {
            AppCompatDelegate.setDefaultNightMode(nightMode)
        }
    })

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

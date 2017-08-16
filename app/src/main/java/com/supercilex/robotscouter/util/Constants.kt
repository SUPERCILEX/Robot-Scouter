package com.supercilex.robotscouter.util

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.support.v4.app.ActivityManagerCompat
import com.firebase.ui.auth.AuthUI
import com.google.firebase.database.DatabaseReference
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher
import com.supercilex.robotscouter.BuildConfig
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.util.data.ref

const val SINGLE_ITEM = 1

const val APP_LINK_BASE = "https://supercilex.github.io/Robot-Scouter/data/"
const val TEAMS_LINK_BASE = "${APP_LINK_BASE}teams"
const val KEY_QUERY = "key"

/** The list of all supported authentication providers in Firebase Auth UI.  */
val ALL_PROVIDERS: List<AuthUI.IdpConfig> = listOf(
        AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build(),
        AuthUI.IdpConfig.Builder(AuthUI.FACEBOOK_PROVIDER).build(),
        AuthUI.IdpConfig.Builder(AuthUI.TWITTER_PROVIDER).build(),
        AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build(),
        AuthUI.IdpConfig.Builder(AuthUI.PHONE_VERIFICATION_PROVIDER).build())

// *** CAUTION--DO NOT TOUCH! ***
// [START FIREBASE CHILD NAMES]
val FIREBASE_USERS: DatabaseReference = ref.child("users")

// Team
val FIREBASE_TEAMS: DatabaseReference = ref.child("teams")
val FIREBASE_TEAM_INDICES: DatabaseReference = ref.child("team-indices")
const val FIREBASE_TIMESTAMP = "timestamp"

// Scout
val FIREBASE_SCOUTS: DatabaseReference = ref.child("scouts")
val FIREBASE_SCOUT_INDICES: DatabaseReference = ref.child("scout-indices")
const val FIREBASE_METRICS = "metrics"

// Scout views
const val FIREBASE_VALUE = "value"
const val FIREBASE_TYPE = "type"
const val FIREBASE_NAME = "name"
const val FIREBASE_UNIT = "unit"
const val FIREBASE_SELECTED_VALUE_KEY = "selectedValueKey"

// Templates
val FIREBASE_TEMPLATES: DatabaseReference = ref.child("templates")
val FIREBASE_TEMPLATE_INDICES: DatabaseReference = ref.child("template-indices")
val FIREBASE_DEFAULT_TEMPLATES: DatabaseReference = ref.child("default-templates")
const val FIREBASE_TEMPLATE_KEY = "templateKey"

// Prefs
const val FIREBASE_PREFS = "prefs"
const val FIREBASE_PREF_DEFAULT_TEMPLATE_KEY = "defaultTemplateKey"
const val FIREBASE_PREF_NIGHT_MODE = "nightMode"
const val FIREBASE_PREF_UPLOAD_MEDIA_TO_TBA = "uploadMediaToTba"
const val FIREBASE_PREF_HAS_SHOWN_ADD_TEAM_TUTORIAL = "hasShownAddTeamTutorial"
const val FIREBASE_PREF_HAS_SHOWN_SIGN_IN_TUTORIAL = "hasShownSignInTutorial"
const val FIREBASE_PREF_HAS_SHOWN_EXPORT_HINT = "hasShownExportHint"
// [END FIREBASE CHILD NAMES]

val providerAuthority: String by lazy { "${RobotScouter.INSTANCE.packageName}.provider" }
val refWatcher: RefWatcher by lazy { LeakCanary.install(RobotScouter.INSTANCE) }
val isLowRamDevice: Boolean by lazy {
    ActivityManagerCompat.isLowRamDevice(
            RobotScouter.INSTANCE.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
}
val isInTestMode: Boolean by lazy {
    Settings.System.getString(RobotScouter.INSTANCE.contentResolver, "firebase.test.lab") == "true"
}
val debugInfo: String get() =
        """
        |- Robot Scouter version: ${BuildConfig.VERSION_NAME}
        |- Android OS version: ${Build.VERSION.SDK_INT}
        |- User id: $uid
        """.trimMargin()

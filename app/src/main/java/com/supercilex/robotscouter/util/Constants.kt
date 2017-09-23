package com.supercilex.robotscouter.util

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.support.v4.app.ActivityManagerCompat
import com.firebase.ui.auth.AuthUI
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher
import com.supercilex.robotscouter.BuildConfig
import com.supercilex.robotscouter.RobotScouter

const val APP_LINK_BASE = "https://supercilex.github.io/Robot-Scouter/data/"

/** The list of all supported authentication providers in Firebase Auth UI.  */
val ALL_PROVIDERS: List<AuthUI.IdpConfig> = listOf(
        AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build(),
        AuthUI.IdpConfig.Builder(AuthUI.FACEBOOK_PROVIDER).build(),
        AuthUI.IdpConfig.Builder(AuthUI.TWITTER_PROVIDER).build(),
        AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build(),
        AuthUI.IdpConfig.Builder(AuthUI.PHONE_VERIFICATION_PROVIDER).build())

// *** CAUTION--DO NOT TOUCH! ***
// [START FIREBASE CHILD NAMES]
const val FIRESTORE_OWNERS = "owners"
const val FIRESTORE_ACTIVE_TOKENS = "activeTokens"
const val FIRESTORE_PENDING_APPROVALS = "pendingApprovals"
const val FIRESTORE_NAME = "name"
const val FIRESTORE_VALUE = "value"
const val FIRESTORE_TIMESTAMP = "timestamp"

val FIRESTORE_USERS = FirebaseFirestore.getInstance().collection("users")
const val FIRESTORE_LAST_LOGIN = "lastLogin"

// Team
val FIRESTORE_TEAMS = FirebaseFirestore.getInstance().collection("teams")
const val FIRESTORE_NUMBER = "number"

// Scout
const val FIRESTORE_SCOUTS = "scouts"
const val FIRESTORE_METRICS = "metrics"

// Scout views
const val FIRESTORE_POSITION = "position"
const val FIRESTORE_TYPE = "type"
const val FIRESTORE_UNIT = "unit"
const val FIRESTORE_SELECTED_VALUE_ID = "selectedValueId"

// Templates
val FIRESTORE_TEMPLATES = FirebaseFirestore.getInstance().collection("templates")
val FIRESTORE_DEFAULT_TEMPLATES = FirebaseFirestore.getInstance().collection("default-templates")
const val FIRESTORE_TEMPLATE_ID = "templateId"

// Prefs
const val FIRESTORE_PREFS = "prefs"
const val FIRESTORE_PREF_DEFAULT_TEMPLATE_ID = "defaultTemplateId"
const val FIRESTORE_PREF_NIGHT_MODE = "nightMode"
const val FIRESTORE_PREF_UPLOAD_MEDIA_TO_TBA = "uploadMediaToTba"
const val FIRESTORE_PREF_HAS_SHOWN_ADD_TEAM_TUTORIAL = "hasShownAddTeamTutorial"
const val FIRESTORE_PREF_HAS_SHOWN_SIGN_IN_TUTORIAL = "hasShownSignInTutorial"
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

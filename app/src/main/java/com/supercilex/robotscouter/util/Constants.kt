package com.supercilex.robotscouter.util

import android.content.Context
import android.os.Build
import com.firebase.ui.auth.AuthUI
import com.google.firebase.database.DatabaseReference
import com.supercilex.robotscouter.BuildConfig
import com.supercilex.robotscouter.util.data.DefaultTemplateLiveData
import com.supercilex.robotscouter.util.data.TeamsLiveData
import com.supercilex.robotscouter.util.data.TemplatesLiveData
import com.supercilex.robotscouter.util.data.ref
import kotlin.properties.Delegates

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

// Scout template
val FIREBASE_DEFAULT_TEMPLATE: DatabaseReference = ref.child("default-template")
val FIREBASE_TEMPLATES: DatabaseReference = ref.child("templates")
const val FIREBASE_TEMPLATE_KEY = "templateKey"
const val FIREBASE_TEMPLATE_INDICES = "template-indices"
// [END FIREBASE CHILD NAMES]

var teamsListener: TeamsLiveData by Delegates.notNull()
    private set
val defaultTemplateListener = DefaultTemplateLiveData()
val templatesListener = TemplatesLiveData()

var providerAuthority: String by Delegates.notNull()
    private set

fun initConstants(context: Context) {
    teamsListener = TeamsLiveData(context)
    providerAuthority = "${context.packageName}.provider"
}

fun getDebugInfo(): String = """
        |- Robot Scouter version: ${BuildConfig.VERSION_NAME}
        |- Android OS version: ${Build.VERSION.SDK_INT}
        |- User id: $uid
        """.trimMargin()

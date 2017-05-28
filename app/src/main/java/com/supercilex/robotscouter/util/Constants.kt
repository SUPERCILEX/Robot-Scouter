package com.supercilex.robotscouter.util

import android.content.Context
import android.os.Build
import com.firebase.ui.auth.AuthUI
import com.google.firebase.database.DatabaseReference
import com.supercilex.robotscouter.BuildConfig
import com.supercilex.robotscouter.ui.AuthHelper
import com.supercilex.robotscouter.util.Constants.sFirebaseScoutTemplates
import kotlin.properties.Delegates

// TODO remove @JvmField once all of RS is converted to Kotlin

@JvmField val MANAGER_STATE = "manager_state"
@JvmField val ITEM_COUNT = "count"
@JvmField val SINGLE_ITEM = 1
@JvmField val TWO_ITEMS = 2

@JvmField val APP_LINK_BASE = "https://supercilex.github.io/Robot-Scouter/?"
@JvmField val TEAM_QUERY_KEY = "team"

/** The list of all supported authentication providers in Firebase Auth UI.  */
@JvmField val ALL_PROVIDERS: List<AuthUI.IdpConfig> = listOf(
        AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build(),
        AuthUI.IdpConfig.Builder(AuthUI.FACEBOOK_PROVIDER).build(),
        AuthUI.IdpConfig.Builder(AuthUI.TWITTER_PROVIDER).build(),
        AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build())

// *** CAUTION--DO NOT TOUCH! ***
// [START FIREBASE CHILD NAMES]
@JvmField val FIREBASE_USERS: DatabaseReference = DatabaseHelper.getRef().child("users")

// Team
@JvmField val FIREBASE_TEAMS: DatabaseReference = DatabaseHelper.getRef().child("teams")
@JvmField val FIREBASE_TEAM_INDICES: DatabaseReference = DatabaseHelper.getRef().child("team-indices")
@JvmField val FIREBASE_TIMESTAMP = "timestamp"

// Scout
@JvmField val FIREBASE_SCOUTS: DatabaseReference = DatabaseHelper.getRef().child("scouts")
@JvmField val FIREBASE_SCOUT_INDICES: DatabaseReference = DatabaseHelper.getRef().child("scout-indices")
@JvmField val FIREBASE_METRICS = "metrics"

// Scout views
@JvmField val FIREBASE_VALUE = "value"
@JvmField val FIREBASE_TYPE = "type"
@JvmField val FIREBASE_NAME = "name"
@JvmField val FIREBASE_UNIT = "unit"
@JvmField val FIREBASE_SELECTED_VALUE_KEY = "selectedValueKey"

// Scout template
@JvmField val FIREBASE_DEFAULT_TEMPLATE: DatabaseReference = DatabaseHelper.getRef().child("default-template")
@JvmField val FIREBASE_SCOUT_TEMPLATES: DatabaseReference = DatabaseHelper.getRef().child("scout-templates")
@JvmField val FIREBASE_TEMPLATE_KEY = "templateKey"
@JvmField val SCOUT_TEMPLATE_INDICES = "scoutTemplateIndices"
// [END FIREBASE CHILD NAMES]

var providerAuthority: String by Delegates.notNull<String>()
@JvmField var providerAuthorityJava: String? = null // TODO remove

fun initConstants(context: Context) {
    providerAuthority = context.packageName + ".provider"
    providerAuthorityJava = providerAuthority
}

fun getScoutMetrics(key: String): DatabaseReference =
        FIREBASE_SCOUTS.child(key).child(FIREBASE_METRICS)

fun getDebugInfo(): String = "* Robot Scouter version: " + BuildConfig.VERSION_NAME + "\n" +
        "* Android OS version: " + Build.VERSION.SDK_INT + "\n" +
        "* User id: " + AuthHelper.getUid() + "\n" +
        "* Scout template keys: " + sFirebaseScoutTemplates.toString()

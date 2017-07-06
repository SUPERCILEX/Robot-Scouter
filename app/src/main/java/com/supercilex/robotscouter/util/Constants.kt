package com.supercilex.robotscouter.util

import android.content.Context
import android.os.Build
import com.firebase.ui.auth.AuthUI
import com.google.firebase.database.DatabaseReference
import com.supercilex.robotscouter.BuildConfig
import com.supercilex.robotscouter.util.Constants.sFirebaseScoutTemplates
import kotlin.properties.Delegates

// TODO remove all @JvmField annotations in all classes once all of RS is converted to Kotlin

const val SINGLE_ITEM = 1
const val TWO_ITEMS = 2

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
@JvmField val FIREBASE_USERS: DatabaseReference = DatabaseHelper.getRef().child("users")

// Team
@JvmField val FIREBASE_TEAMS: DatabaseReference = DatabaseHelper.getRef().child("teams")
@JvmField val FIREBASE_TEAM_INDICES: DatabaseReference = DatabaseHelper.getRef().child("team-indices")
const val FIREBASE_TIMESTAMP = "timestamp"

// Scout
@JvmField val FIREBASE_SCOUTS: DatabaseReference = DatabaseHelper.getRef().child("scouts")
@JvmField val FIREBASE_SCOUT_INDICES: DatabaseReference = DatabaseHelper.getRef().child("scout-indices")
const val FIREBASE_METRICS = "metrics"

// Scout views
const val FIREBASE_VALUE = "value"
const val FIREBASE_TYPE = "type"
const val FIREBASE_NAME = "name"
const val FIREBASE_UNIT = "unit"
const val FIREBASE_SELECTED_VALUE_KEY = "selectedValueKey"

// Scout template
@JvmField val FIREBASE_DEFAULT_TEMPLATE: DatabaseReference = DatabaseHelper.getRef().child("default-template")
@JvmField val FIREBASE_SCOUT_TEMPLATES: DatabaseReference = DatabaseHelper.getRef().child("scout-templates")
const val FIREBASE_TEMPLATE_KEY = "templateKey"
const val SCOUT_TEMPLATE_INDICES = "scoutTemplateIndices"
// [END FIREBASE CHILD NAMES]

var providerAuthority: String by Delegates.notNull<String>()
@JvmField var providerAuthorityJava: String? = null // TODO remove

fun initConstants(context: Context) {
    providerAuthority = context.packageName + ".provider"
    providerAuthorityJava = providerAuthority
}

fun getDebugInfo(): String = "* Robot Scouter version: " + BuildConfig.VERSION_NAME + "\n" +
        "* Android OS version: " + Build.VERSION.SDK_INT + "\n" +
        "* User id: " + getUid() + "\n" +
        "* Scout template keys: " + sFirebaseScoutTemplates.toString()

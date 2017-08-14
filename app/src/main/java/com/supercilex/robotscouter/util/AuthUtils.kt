package com.supercilex.robotscouter.util

import android.app.Activity
import android.content.Intent
import android.support.v4.app.Fragment
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crash.FirebaseCrash
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.client.AccountMergeService

const val RC_SIGN_IN = 100

val user get() = FirebaseAuth.getInstance().currentUser

val uid get() = user?.uid

val isSignedIn get() = user != null

val isFullUser get() = isSignedIn && !user!!.isAnonymous

private val signInIntent: Intent get() = AuthUI.getInstance().createSignInIntentBuilder()
        .setAvailableProviders(
                if (isInTestMode) listOf(AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build())
                else ALL_PROVIDERS)
        .setLogo(R.drawable.ic_logo)
        .setPrivacyPolicyUrl("https://supercilex.github.io/Robot-Scouter/privacy-policy/")
        .setIsAccountLinkingEnabled(true, AccountMergeService::class.java)
        .build()

fun onSignedIn() = TaskCompletionSource<FirebaseAuth>().also {
    FirebaseAuth.getInstance().addAuthStateListener(object : FirebaseAuth.AuthStateListener {
        override fun onAuthStateChanged(auth: FirebaseAuth) {
            if (auth.currentUser == null) {
                signInAnonymouslyDbInit()
            } else {
                it.trySetResult(auth)
                auth.removeAuthStateListener(this)
            }
        }
    })
}.task

fun signIn(activity: Activity) = activity.startActivityForResult(signInIntent, RC_SIGN_IN)

fun signIn(fragment: Fragment) = fragment.startActivityForResult(signInIntent, RC_SIGN_IN)

fun signInAnonymouslyDbInit() = FirebaseAuth.getInstance().signInAnonymously().addOnSuccessListener {
    DatabaseInitializer()
}

private class DatabaseInitializer : ValueEventListener, OnSuccessListener<Nothing?> {
    init {
        fetchAndActivate().addOnSuccessListener(this)
        FIREBASE_SCOUT_INDICES.addListenerForSingleValueEvent(this)
    }

    override fun onSuccess(nothing: Nothing?) {
        if (FirebaseRemoteConfig.getInstance().getBoolean(SHOULD_CACHE_DB)) {
            FIREBASE_TEMPLATES.addListenerForSingleValueEvent(this)
        }
    }

    override fun onDataChange(snapshot: DataSnapshot) {
        // This allows the database to work offline without any setup
    }

    override fun onCancelled(error: DatabaseError) = FirebaseCrash.report(error.toException())

    private companion object {
        const val SHOULD_CACHE_DB = "should_cache_db"
    }
}

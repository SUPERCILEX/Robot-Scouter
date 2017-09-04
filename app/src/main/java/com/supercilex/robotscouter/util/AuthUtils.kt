package com.supercilex.robotscouter.util

import android.app.Activity
import android.content.Intent
import android.support.v4.app.Fragment
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.auth.FirebaseAuth
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
                FirebaseAuth.getInstance().signInAnonymously()
            } else {
                it.trySetResult(auth)
                auth.removeAuthStateListener(this)
            }
        }
    })
}.task

fun signIn(activity: Activity) = activity.startActivityForResult(signInIntent, RC_SIGN_IN)

fun signIn(fragment: Fragment) = fragment.startActivityForResult(signInIntent, RC_SIGN_IN)

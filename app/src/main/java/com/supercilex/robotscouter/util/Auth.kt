package com.supercilex.robotscouter.util

import android.app.Activity
import android.content.Intent
import android.support.v4.app.Fragment
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.client.AccountMergeService
import kotlin.coroutines.experimental.suspendCoroutine

const val RC_SIGN_IN = 100

val user get() = FirebaseAuth.getInstance().currentUser
val uid get() = user?.uid
val isSignedIn get() = user != null
val isFullUser get() = isSignedIn && !user!!.isAnonymous

private val signInIntent: Intent
    get() = AuthUI.getInstance().createSignInIntentBuilder()
            .setAvailableProviders(if (isInTestMode) {
                listOf(AuthUI.IdpConfig.GoogleBuilder().build())
            } else {
                allProviders
            })
            .setTheme(R.style.RobotScouter)
            .setLogo(R.drawable.ic_logo)
            .setPrivacyPolicyUrl("https://supercilex.github.io/Robot-Scouter/privacy-policy/")
            .setIsAccountLinkingEnabled(true, AccountMergeService::class.java)
            .build()

suspend fun onSignedIn(): FirebaseUser = suspendCoroutine {
    FirebaseAuth.getInstance().addAuthStateListener(object : FirebaseAuth.AuthStateListener {
        override fun onAuthStateChanged(auth: FirebaseAuth) {
            val user = auth.currentUser
            if (user == null) {
                FirebaseAuth.getInstance().signInAnonymously()
            } else {
                auth.removeAuthStateListener(this)
                it.resume(user)
            }
        }
    })
}

fun signIn(activity: Activity) = activity.startActivityForResult(signInIntent, RC_SIGN_IN)

fun signIn(fragment: Fragment) = fragment.startActivityForResult(signInIntent, RC_SIGN_IN)

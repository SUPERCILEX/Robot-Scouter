package com.supercilex.robotscouter.shared.client

import android.app.Activity
import android.content.Intent
import android.support.v4.app.Fragment
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.isInTestMode
import com.supercilex.robotscouter.shared.R
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.experimental.suspendCoroutine

const val RC_SIGN_IN = 100

/** The list of all supported authentication providers in Firebase Auth UI.  */
private val allProviders: List<AuthUI.IdpConfig> = listOf(
        AuthUI.IdpConfig.GoogleBuilder().build(),
        AuthUI.IdpConfig.FacebookBuilder().build(),
        AuthUI.IdpConfig.TwitterBuilder().build(),
        AuthUI.IdpConfig.EmailBuilder().build(),
        AuthUI.IdpConfig.PhoneBuilder().build()
)

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

suspend fun onSignedIn(): FirebaseUser = suspendCoroutine { cont ->
    FirebaseAuth.getInstance().addAuthStateListener(object : FirebaseAuth.AuthStateListener {
        private val called = AtomicBoolean()

        override fun onAuthStateChanged(auth: FirebaseAuth) {
            val user = auth.currentUser
            if (user == null) {
                AuthUI.getInstance().silentSignIn(RobotScouter, allProviders).continueWithTask {
                    if (it.isSuccessful) {
                        it
                    } else {
                        // Ignore any exceptions since we don't care about credential fetch errors
                        FirebaseAuth.getInstance().signInAnonymously()
                    }
                }.addOnCompleteListener {
                    if (called.compareAndSet(false, true)) {
                        if (it.isSuccessful) {
                            cont.resume(it.result.user)
                        } else {
                            cont.resumeWithException(it.exception!!)
                        }
                    }
                }
            } else {
                if (called.compareAndSet(false, true)) {
                    cont.resume(user)
                }
            }
            auth.removeAuthStateListener(this)
        }
    })
}

fun Activity.startSignIn() = startActivityForResult(signInIntent, RC_SIGN_IN)

fun Fragment.startSignIn() = startActivityForResult(signInIntent, RC_SIGN_IN)

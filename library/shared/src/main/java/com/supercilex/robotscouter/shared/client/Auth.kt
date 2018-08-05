package com.supercilex.robotscouter.shared.client

import android.app.Activity
import android.content.Intent
import androidx.fragment.app.Fragment
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.asTask
import com.supercilex.robotscouter.core.await
import com.supercilex.robotscouter.core.data.user
import com.supercilex.robotscouter.core.isInTestMode
import com.supercilex.robotscouter.shared.R
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock

const val RC_SIGN_IN = 100

/** The list of all supported authentication providers in Firebase Auth UI.  */
private val allProviders: List<AuthUI.IdpConfig> = listOf(
        AuthUI.IdpConfig.GoogleBuilder().build(),
        AuthUI.IdpConfig.FacebookBuilder().build(),
        AuthUI.IdpConfig.TwitterBuilder().build(),
        AuthUI.IdpConfig.GitHubBuilder().build(),
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
            .setTosAndPrivacyPolicyUrls(
                    "https://supercilex.github.io/Robot-Scouter/tos/",
                    "https://supercilex.github.io/Robot-Scouter/privacy-policy/"
            )
            .setIsAccountLinkingEnabled(true, AccountMergeService::class.java)
            .build()

private val signInLock = Mutex()

suspend fun onSignedIn(): FirebaseUser = signInLock.withLock {
    user ?: try {
        AuthUI.getInstance().silentSignIn(RobotScouter, allProviders).await()
    } catch (e: Exception) {
        // Ignore any exceptions since we don't care about credential fetch errors
        FirebaseAuth.getInstance().signInAnonymously().await()
    }.user
}

fun onSignedInTask() = async { onSignedIn() }.asTask()

fun Activity.startSignIn() = startActivityForResult(signInIntent, RC_SIGN_IN)

fun Fragment.startSignIn() = startActivityForResult(signInIntent, RC_SIGN_IN)

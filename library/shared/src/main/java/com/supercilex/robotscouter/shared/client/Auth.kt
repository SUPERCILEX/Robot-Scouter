package com.supercilex.robotscouter.shared.client

import android.app.Activity
import androidx.fragment.app.Fragment
import com.facebook.FacebookSdk
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.util.GoogleApiUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.await
import com.supercilex.robotscouter.core.data.user
import com.supercilex.robotscouter.core.isInTestMode
import com.supercilex.robotscouter.shared.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.asTask
import kotlinx.coroutines.withContext

const val RC_SIGN_IN = 100

/** The list of all supported authentication providers in Firebase Auth UI. */
private val allProviders: List<AuthUI.IdpConfig> = listOf(
        AuthUI.IdpConfig.GoogleBuilder().build(),
        AuthUI.IdpConfig.FacebookBuilder().build(),
        AuthUI.IdpConfig.TwitterBuilder().build(),
        AuthUI.IdpConfig.GitHubBuilder().build(),
        AuthUI.IdpConfig.EmailBuilder().build(),
        AuthUI.IdpConfig.PhoneBuilder().build()
)

private val unlinkedProviders: List<AuthUI.IdpConfig>
    get() {
        val existingProviders = user?.providers.orEmpty()
        return allProviders.filterNot { existingProviders.contains(it.providerId) }
    }

private val signInBuilder
    get() = AuthUI.getInstance().createSignInIntentBuilder()
            .setAvailableProviders(if (isInTestMode) {
                listOf(AuthUI.IdpConfig.GoogleBuilder().build())
            } else {
                initFacebook()
                allProviders
            })
            .setTheme(R.style.RobotScouter)
            .setLogo(R.drawable.ic_logo)
            .setTosAndPrivacyPolicyUrls(
                    "https://supercilex.github.io/Robot-Scouter/tos/",
                    "https://supercilex.github.io/Robot-Scouter/privacy-policy/"
            )
            .setIsAccountLinkingEnabled(true, AccountMergeService::class.java)

private val signInLock = Mutex()

suspend fun onSignedIn(): FirebaseUser = signInLock.withLock {
    user ?: try {
        AuthUI.getInstance().silentSignIn(RobotScouter, allProviders).await()
    } catch (e: Exception) {
        // Ignore any exceptions since we don't care about credential fetch errors
        FirebaseAuth.getInstance().signInAnonymously().await()
    }.user
}

fun onSignedInTask() = GlobalScope.async { onSignedIn() }.asTask()

fun Activity.startSignIn() = startActivityForResult(signInBuilder.build(), RC_SIGN_IN)

fun Fragment.startLinkingSignIn() {
    val activity = requireActivity()
    GoogleApiUtils.getCredentialsClient(activity)
            .disableAutoSignIn()
            .addOnCompleteListener(activity) {
                startActivityForResult(
                        signInBuilder.setAvailableProviders(unlinkedProviders).build(),
                        RC_SIGN_IN
                )
            }
}

suspend fun idpSignOut() {
    // Move to background since signOut sometimes does disk I/O
    withContext(Dispatchers.IO) {
        initFacebook()
        AuthUI.getInstance().signOut(RobotScouter).await()
    }
}

private fun initFacebook() {
    @Suppress("DEPRECATION") FacebookSdk.sdkInitialize(RobotScouter)
}

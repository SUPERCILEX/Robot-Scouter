package com.supercilex.robotscouter

import android.app.Activity
import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.supercilex.robotscouter.core.asTask
import com.supercilex.robotscouter.core.data.hasShownSignInTutorial
import com.supercilex.robotscouter.core.data.isFullUser
import com.supercilex.robotscouter.core.data.isSignedIn
import com.supercilex.robotscouter.core.data.logLoginEvent
import com.supercilex.robotscouter.core.ui.OnActivityResult
import com.supercilex.robotscouter.core.ui.longSnackbar
import com.supercilex.robotscouter.shared.client.RC_SIGN_IN
import com.supercilex.robotscouter.shared.client.onSignedIn
import com.supercilex.robotscouter.shared.client.startSignIn
import kotlinx.coroutines.experimental.async
import org.jetbrains.anko.find

internal class AuthHelper(private val activity: AppCompatActivity) : (View) -> Unit,
        DefaultLifecycleObserver, FirebaseAuth.AuthStateListener, OnActivityResult {
    private val rootView: View = activity.find(R.id.root)

    private var signInMenuItem: MenuItem? = null

    init {
        activity.lifecycle.addObserver(this)
    }

    fun init(): Task<*> = if (isSignedIn) Tasks.forResult(null) else signInAnonymously()

    fun initMenu(menu: Menu) {
        signInMenuItem = menu.findItem(R.id.action_sign_in)
        updateMenuState()
    }

    override fun onStart(owner: LifecycleOwner) =
            FirebaseAuth.getInstance().addAuthStateListener(this)

    override fun onStop(owner: LifecycleOwner) =
            FirebaseAuth.getInstance().removeAuthStateListener(this)

    override fun onAuthStateChanged(auth: FirebaseAuth) = updateMenuState()

    fun signIn() = activity.startSignIn()

    private fun signInAnonymously() = async { onSignedIn() }.asTask()
            .addOnFailureListener(activity) {
                longSnackbar(
                        rootView,
                        R.string.anonymous_sign_in_failed_message,
                        R.string.sign_in_title,
                        this
                )
            }

    fun showSignInResolution() {
        longSnackbar(rootView, R.string.sign_in_required, R.string.sign_in_title, this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == Activity.RESULT_OK) {
                longSnackbar(rootView, R.string.signed_in_message)
                hasShownSignInTutorial = true

                logLoginEvent()
            } else {
                val response = IdpResponse.fromResultIntent(data) ?: return

                if (response.error?.errorCode == ErrorCodes.NO_NETWORK) {
                    longSnackbar(
                            rootView,
                            R.string.no_connection,
                            R.string.sign_in_try_again_title,
                            this
                    )
                    return
                }

                longSnackbar(
                        rootView,
                        R.string.sign_in_failed_message,
                        R.string.sign_in_try_again_title,
                        this
                )
            }
        }
    }

    override fun invoke(v: View) = signIn()

    private fun updateMenuState() {
        signInMenuItem?.isVisible = !isFullUser
    }
}

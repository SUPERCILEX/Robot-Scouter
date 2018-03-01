package com.supercilex.robotscouter.ui.teamlist

import android.app.Activity
import android.arch.lifecycle.DefaultLifecycleObserver
import android.arch.lifecycle.LifecycleOwner
import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.util.RC_SIGN_IN
import com.supercilex.robotscouter.util.await
import com.supercilex.robotscouter.util.data.hasShownSignInTutorial
import com.supercilex.robotscouter.util.isFullUser
import com.supercilex.robotscouter.util.isSignedIn
import com.supercilex.robotscouter.util.logLoginEvent
import com.supercilex.robotscouter.util.ui.OnActivityResult
import kotterknife.bindView
import org.jetbrains.anko.design.longSnackbar
import com.supercilex.robotscouter.util.signIn as startSignInIntent

class AuthHelper(private val activity: TeamListActivity) : (View) -> Unit,
        DefaultLifecycleObserver, FirebaseAuth.AuthStateListener, OnActivityResult {
    private val rootView: View by activity.bindView(R.id.root)

    private var signInMenuItem: MenuItem? = null

    init {
        activity.lifecycle.addObserver(this)
    }

    suspend fun init() {
        if (isSignedIn) return
        signInAnonymously().await()
    }

    fun initMenu(menu: Menu) {
        signInMenuItem = menu.findItem(R.id.action_sign_in)
        updateMenuState()
    }

    override fun onStart(owner: LifecycleOwner) =
            FirebaseAuth.getInstance().addAuthStateListener(this)

    override fun onStop(owner: LifecycleOwner) =
            FirebaseAuth.getInstance().removeAuthStateListener(this)

    override fun onAuthStateChanged(auth: FirebaseAuth) = updateMenuState()

    fun signIn() = startSignInIntent(activity)

    private fun signInAnonymously() = FirebaseAuth.getInstance().signInAnonymously()
            .addOnFailureListener(activity) {
                longSnackbar(
                        rootView,
                        R.string.team_anonymous_sign_in_failed_message,
                        R.string.team_sign_in_title,
                        this
                )
            }

    fun showSignInResolution() {
        longSnackbar(rootView, R.string.sign_in_required, R.string.team_sign_in_title, this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == Activity.RESULT_OK) {
                longSnackbar(rootView, R.string.team_signed_in_message)
                hasShownSignInTutorial = true

                logLoginEvent()
            } else {
                val response: IdpResponse = IdpResponse.fromResultIntent(data) ?: return

                if (response.errorCode == ErrorCodes.NO_NETWORK) {
                    longSnackbar(
                            rootView,
                            R.string.no_connection,
                            R.string.team_sign_in_try_again_title,
                            this
                    )
                    return
                }

                longSnackbar(
                        rootView,
                        R.string.team_sign_in_failed_message,
                        R.string.team_sign_in_try_again_title,
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

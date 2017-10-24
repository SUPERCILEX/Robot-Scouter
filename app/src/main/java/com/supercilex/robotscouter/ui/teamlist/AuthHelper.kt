package com.supercilex.robotscouter.ui.teamlist

import android.app.Activity
import android.arch.lifecycle.DefaultLifecycleObserver
import android.arch.lifecycle.LifecycleOwner
import android.content.Intent
import android.support.design.widget.Snackbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.util.RC_SIGN_IN
import com.supercilex.robotscouter.util.data.hasShownSignInTutorial
import com.supercilex.robotscouter.util.isFullUser
import com.supercilex.robotscouter.util.isSignedIn
import com.supercilex.robotscouter.util.logLoginEvent
import kotterknife.bindView

class AuthHelper(private val activity: TeamListActivity) : View.OnClickListener,
        DefaultLifecycleObserver, FirebaseAuth.AuthStateListener {
    private val rootView: View by activity.bindView(R.id.root)

    private var signInMenuItem: MenuItem? = null

    init {
        activity.lifecycle.addObserver(this)
    }

    fun init(): Task<Nothing> =
            if (isSignedIn) Tasks.forResult(null) else signInAnonymously().continueWith { null }

    fun initMenu(menu: Menu) {
        signInMenuItem = menu.findItem(R.id.action_sign_in)
        updateMenuState()
    }

    override fun onStart(owner: LifecycleOwner) =
            FirebaseAuth.getInstance().addAuthStateListener(this)

    override fun onStop(owner: LifecycleOwner) =
            FirebaseAuth.getInstance().removeAuthStateListener(this)

    override fun onAuthStateChanged(auth: FirebaseAuth) = updateMenuState()

    fun signIn() = com.supercilex.robotscouter.util.signIn(activity)

    private fun signInAnonymously() = FirebaseAuth.getInstance().signInAnonymously()
            .addOnFailureListener(activity) {
                Snackbar.make(rootView,
                              R.string.team_anonymous_sign_in_failed_message,
                              Snackbar.LENGTH_LONG)
                        .setAction(R.string.team_sign_in_title, this)
                        .show()
            }

    fun showSignInResolution() =
            Snackbar.make(rootView, R.string.sign_in_required, Snackbar.LENGTH_LONG)
                    .setAction(R.string.team_sign_in_title, this)
                    .show()

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == Activity.RESULT_OK) {
                Snackbar.make(rootView, R.string.team_signed_in_message, Snackbar.LENGTH_LONG)
                        .show()
                hasShownSignInTutorial = true

                logLoginEvent()
            } else {
                val response: IdpResponse = IdpResponse.fromResultIntent(data) ?: return

                if (response.errorCode == ErrorCodes.NO_NETWORK) {
                    Snackbar.make(rootView, R.string.no_connection, Snackbar.LENGTH_LONG)
                            .setAction(R.string.team_sign_in_try_again_title, this)
                            .show()
                    return
                }

                Snackbar.make(rootView, R.string.team_sign_in_failed_message, Snackbar.LENGTH_LONG)
                        .setAction(R.string.team_sign_in_try_again_title, this)
                        .show()
            }
        }
    }

    override fun onClick(v: View) = signIn()

    private fun updateMenuState() {
        signInMenuItem?.isVisible = !isFullUser
    }
}

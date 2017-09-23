package com.supercilex.robotscouter.ui.teamlist

import android.app.Activity
import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.OnLifecycleEvent
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

class AuthHelper(private val activity: TeamListActivity) : View.OnClickListener,
        LifecycleObserver, FirebaseAuth.AuthStateListener {
    private val rootView: View = activity.findViewById(R.id.root)

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

    @OnLifecycleEvent(Lifecycle.Event.ON_ANY)
    fun onStateChange(source: LifecycleOwner, event: Lifecycle.Event) {
        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (event) {
            Lifecycle.Event.ON_START -> FirebaseAuth.getInstance().addAuthStateListener(this)
            Lifecycle.Event.ON_STOP -> FirebaseAuth.getInstance().removeAuthStateListener(this)
            Lifecycle.Event.ON_DESTROY -> source.lifecycle.removeObserver(this)
        }
    }

    override fun onAuthStateChanged(auth: FirebaseAuth) = updateMenuState()

    fun signIn() = com.supercilex.robotscouter.util.signIn(activity)

    private fun signInAnonymously() = FirebaseAuth.getInstance().signInAnonymously()
            .addOnFailureListener(activity) {
                Snackbar.make(rootView, R.string.anonymous_sign_in_failed, Snackbar.LENGTH_LONG)
                        .setAction(R.string.sign_in, this)
                        .show()
            }

    fun showSignInResolution() =
            Snackbar.make(rootView, R.string.sign_in_required, Snackbar.LENGTH_LONG)
                    .setAction(R.string.sign_in, this)
                    .show()

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == Activity.RESULT_OK) {
                Snackbar.make(rootView, R.string.signed_in, Snackbar.LENGTH_LONG).show()
                hasShownSignInTutorial = true

                logLoginEvent()
            } else {
                val response: IdpResponse = IdpResponse.fromResultIntent(data) ?: return

                if (response.errorCode == ErrorCodes.NO_NETWORK) {
                    Snackbar.make(rootView, R.string.no_connection, Snackbar.LENGTH_LONG)
                            .setAction(R.string.try_again, this)
                            .show()
                    return
                }

                Snackbar.make(rootView, R.string.sign_in_failed, Snackbar.LENGTH_LONG)
                        .setAction(R.string.try_again, this)
                        .show()
            }
        }
    }

    override fun onClick(v: View) = signIn()

    private fun updateMenuState() {
        signInMenuItem?.isVisible = !isFullUser
    }
}

package com.supercilex.robotscouter.ui.teamlist

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.firebase.ui.auth.util.PlayServicesHelper
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.supercilex.robotscouter.BuildConfig
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.util.TeamHelper
import com.supercilex.robotscouter.ui.scout.ScoutActivity
import com.supercilex.robotscouter.ui.scout.ScoutListFragmentBase.KEY_SCOUT_ARGS
import com.supercilex.robotscouter.util.fetchAndActivate
import com.supercilex.robotscouter.util.isOffline
import com.supercilex.robotscouter.util.isSignedIn
import com.supercilex.robotscouter.util.isTabletMode
import com.supercilex.robotscouter.util.logSelectTeamEvent
import com.supercilex.robotscouter.util.setHasShownAddTeamTutorial
import com.supercilex.robotscouter.util.setHasShownSignInTutorial
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt

@SuppressLint("GoogleAppIndexingApiWarning")
class TeamListActivity : AppCompatActivity(), View.OnClickListener, TeamSelectionListener, OnSuccessListener<Void> {
    private val teamListFragment: TeamListFragment by lazy {
        supportFragmentManager.findFragmentByTag(TeamListFragment.TAG) as TeamListFragment
    }
    private val authHelper: AuthHelper by lazy { AuthHelper(this) }
    private val addTeamPrompt: MaterialTapTargetPrompt? by lazy { showCreateFirstTeamPrompt(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.RobotScouter_NoActionBar)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_team_list)
        setSupportActionBar(findViewById(R.id.toolbar))

        findViewById<View>(R.id.fab).setOnClickListener(this)
        addTeamPrompt
        authHelper.init().addOnSuccessListener(this) {
            handleIntent(intent)
            intent = Intent() // Consume Intent
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        PlayServicesHelper.makePlayServicesAvailable(this, API_AVAILABILITY_RC) { finish() }
        val result = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
        GoogleApiAvailability.getInstance().showErrorNotification(this, result)

        fetchAndActivate().addOnSuccessListener(this, this)
    }

    override fun onSuccess(aVoid: Void?) {
        val minimum = FirebaseRemoteConfig.getInstance().getDouble(MINIMUM_APP_VERSION_KEY)
        if (BuildConfig.VERSION_CODE < minimum && !isOffline(this)) {
            UpdateDialog.show(supportFragmentManager)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.team_list, menu)
        authHelper.initMenu(menu)
        Handler().post { showSignInPrompt(this) }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_sign_in -> authHelper.signIn()
            R.id.action_sign_out -> authHelper.signOut()
            R.id.action_donate -> DonateDialog.show(supportFragmentManager)
            R.id.action_licenses -> LicensesDialog.show(supportFragmentManager)
            R.id.action_about -> AboutDialog.show(supportFragmentManager)
            else -> return false
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (authHelper.onActivityResult(requestCode, resultCode, data) && addTeamPrompt != null) {
            addTeamPrompt!!.dismiss()
            setHasShownAddTeamTutorial(this, true)
            setHasShownSignInTutorial(this, true)
        }
        if (requestCode == RC_SCOUT && resultCode == Activity.RESULT_OK) {
            onTeamSelected(data!!.getBundleExtra(KEY_SCOUT_ARGS), true)
        }
    }

    override fun onBackPressed() {
        if (!teamListFragment.onBackPressed()) super.onBackPressed()
    }

    override fun onClick(v: View) {
        if (v.id == R.id.fab) {
            if (isSignedIn()) {
                NewTeamDialog.show(supportFragmentManager)
            } else {
                authHelper.showSignInResolution()
            }
        }
    }

    override fun onTeamSelected(args: Bundle, restoreOnConfigChange: Boolean) {
        val team = TeamHelper.parse(args).team

        if (isTabletMode(this)) {
            teamListFragment.selectTeam(null)
            teamListFragment.selectTeam(team)
            supportFragmentManager.beginTransaction()
                    .replace(R.id.scouts, TabletScoutListFragment.newInstance(args))
                    .commit()
        } else {
            if (restoreOnConfigChange) {
                startActivityForResult(ScoutActivity.createIntent(this, args), RC_SCOUT)
            } else {
                startActivity(ScoutActivity.createIntent(this, args))
            }
            teamListFragment.selectTeam(null)
        }

        logSelectTeamEvent(team.number)
    }

    private fun handleIntent(intent: Intent) {
        intent.extras?.let {
            when {
                it.containsKey(KEY_SCOUT_ARGS) -> onTeamSelected(it.getBundle(KEY_SCOUT_ARGS), true)
                it.containsKey(DONATE_EXTRA) -> DonateDialog.show(supportFragmentManager)
                it.containsKey(UPDATE_EXTRA) -> UpdateDialog.showStoreListing(this)
            }
        }

        intent.data?.let {
            if (it.toString() == ADD_SCOUT_INTENT) NewTeamDialog.show(supportFragmentManager)
        }
    }

    private companion object {
        const val RC_SCOUT = 744
        const val API_AVAILABILITY_RC = 65
        const val MINIMUM_APP_VERSION_KEY = "minimum_app_version"

        const val DONATE_EXTRA = "donate_extra"
        const val UPDATE_EXTRA = "update_extra"
        const val ADD_SCOUT_INTENT = "add_scout"
    }
}

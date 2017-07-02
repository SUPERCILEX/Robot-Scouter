package com.supercilex.robotscouter.ui.teamlist

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import android.view.View
import com.firebase.ui.auth.util.PlayServicesHelper
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.supercilex.robotscouter.BuildConfig
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.util.TeamHelper
import com.supercilex.robotscouter.ui.AuthHelper
import com.supercilex.robotscouter.ui.scout.ScoutActivity
import com.supercilex.robotscouter.ui.scout.ScoutListFragmentBase.KEY_SCOUT_ARGS
import com.supercilex.robotscouter.util.fetchAndActivate
import com.supercilex.robotscouter.util.isOffline
import com.supercilex.robotscouter.util.isSignedIn
import com.supercilex.robotscouter.util.isTabletMode
import com.supercilex.robotscouter.util.logSelectTeamEvent
import com.supercilex.robotscouter.util.setHasShownAddTeamTutorial
import com.supercilex.robotscouter.util.setHasShownSignInTutorial


@SuppressLint("GoogleAppIndexingApiWarning")
class TeamListActivity : AppCompatActivity(), View.OnClickListener, TeamSelectionListener, OnSuccessListener<Nothing>, NavigationView.OnNavigationItemSelectedListener {
    private val teamListFragment by lazy {
        supportFragmentManager.findFragmentByTag(TeamListFragment.TAG) as TeamListFragment
    }
    private val authHelper by lazy { AuthHelper(this) }
    private val addTeamPrompt by lazy { showCreateFirstTeamPrompt(this) }

    private val drawerLayout by lazy { findViewById<DrawerLayout>(R.id.drawer_layout) }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.RobotScouter_NoActionBar_TransparentStatusBar)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_team_list)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(this,
                drawerLayout,
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        val drawer = findViewById<NavigationView>(R.id.drawer)
        drawer.setNavigationItemSelectedListener(this)

        authHelper; addTeamPrompt
        findViewById<View>(R.id.fab).setOnClickListener(this)
    }

    override fun onStart() {
        super.onStart()
        PlayServicesHelper.makePlayServicesAvailable(this, API_AVAILABILITY_RC) { finish() }
        val result = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
        GoogleApiAvailability.getInstance().showErrorNotification(this, result)

        fetchAndActivate().addOnSuccessListener(this, this)
    }

    override fun onSuccess(nothing: Nothing?) {
        val minimum = FirebaseRemoteConfig.getInstance().getDouble(MINIMUM_APP_VERSION_KEY)
        if (BuildConfig.VERSION_CODE < minimum && !isOffline(this)) {
            UpdateDialog.show(supportFragmentManager)
        }
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

    override fun onClick(v: View) {
        if (v.id == R.id.fab) {
            if (isSignedIn()) {
                NewTeamDialog.show(supportFragmentManager)
            } else {
                authHelper.showSignInResolution()
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_sign_in -> authHelper.signIn()
            R.id.action_sign_out -> authHelper.signOut()
            R.id.action_donate -> DonateDialog.show(supportFragmentManager)
            R.id.action_about -> AboutDialog.show(supportFragmentManager)
            R.id.action_licenses -> LicensesDialog.show(supportFragmentManager)
            else -> throw IllegalStateException()
        }

        drawerLayout.closeDrawer(GravityCompat.START)
        return false
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            if (!teamListFragment.onBackPressed()) super.onBackPressed()
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

    companion object {
        private const val RC_SCOUT = 744
        private const val API_AVAILABILITY_RC = 65
        private const val MINIMUM_APP_VERSION_KEY = "minimum_app_version"
    }
}

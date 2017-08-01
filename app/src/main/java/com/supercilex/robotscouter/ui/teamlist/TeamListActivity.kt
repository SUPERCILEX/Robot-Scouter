package com.supercilex.robotscouter.ui.teamlist

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.firebase.ui.auth.util.PlayServicesHelper
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.crash.FirebaseCrash
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.supercilex.robotscouter.BuildConfig
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.ui.scouting.scout.ScoutActivity
import com.supercilex.robotscouter.ui.scouting.template.TemplateEditorActivity
import com.supercilex.robotscouter.util.data.SCOUT_ARGS_KEY
import com.supercilex.robotscouter.util.data.model.parseTeam
import com.supercilex.robotscouter.util.data.setHasShownAddTeamTutorial
import com.supercilex.robotscouter.util.data.setHasShownSignInTutorial
import com.supercilex.robotscouter.util.fetchAndActivate
import com.supercilex.robotscouter.util.isOffline
import com.supercilex.robotscouter.util.isSignedIn
import com.supercilex.robotscouter.util.logSelectTeamEvent
import com.supercilex.robotscouter.util.ui.isInTabletMode

@SuppressLint("GoogleAppIndexingApiWarning")
class TeamListActivity : AppCompatActivity(), View.OnClickListener, NavigationView.OnNavigationItemSelectedListener,
        TeamSelectionListener, OnSuccessListener<Nothing?> {
    private val teamListFragment by lazy {
        supportFragmentManager.findFragmentByTag(TeamListFragment.TAG) as TeamListFragment
    }
    private val authHelper by lazy { AuthHelper(this) }
    private val addTeamPrompt by lazy { showCreateFirstTeamPrompt(this) }

    val drawerToggle: ActionBarDrawerToggle by lazy {
        ActionBarDrawerToggle(this,
                drawerLayout,
                findViewById(R.id.toolbar),
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close)
    }
    private val drawerLayout by lazy { findViewById<DrawerLayout>(R.id.drawer_layout) }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.RobotScouter_NoActionBar_TransparentStatusBar)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_team_list)
        setSupportActionBar(findViewById(R.id.toolbar))

        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()

        findViewById<NavigationView>(R.id.drawer).setNavigationItemSelectedListener(this)

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

    override fun onSuccess(nothing: Nothing?) {
        val minimum = FirebaseRemoteConfig.getInstance().getDouble(MINIMUM_APP_VERSION_KEY)
        if (BuildConfig.VERSION_CODE < minimum && !isOffline()) {
            UpdateDialog.show(supportFragmentManager)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.team_list_menu, menu)
        authHelper.initMenu(menu)
        Handler().post { showSignInPrompt(this) }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = if (item.itemId == R.id.action_sign_in) {
        authHelper.signIn(); true
    } else false

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (authHelper.onActivityResult(requestCode, resultCode, data) && addTeamPrompt != null) {
            addTeamPrompt!!.dismiss()
            setHasShownAddTeamTutorial(this, true)
            setHasShownSignInTutorial(this, true)
        }
        if (requestCode == RC_SCOUT && resultCode == Activity.RESULT_OK) {
            onTeamSelected(data!!.getBundleExtra(SCOUT_ARGS_KEY), true)
        }
    }

    override fun onClick(v: View) {
        if (v.id == R.id.fab) {
            if (isSignedIn) {
                NewTeamDialog.show(supportFragmentManager)
            } else {
                authHelper.showSignInResolution()
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_export_all_teams -> teamListFragment.exportAllTeams()
            R.id.action_edit_templates -> TemplateEditorActivity.start(this)
            R.id.action_donate -> DonateDialog.show(supportFragmentManager)
            R.id.action_sign_out -> authHelper.signOut()
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
        val team = parseTeam(args)

        if (isInTabletMode(this)) {
            teamListFragment.selectTeam(null)
            teamListFragment.selectTeam(team)
            supportFragmentManager.beginTransaction()
                    .replace(R.id.scout_list, TabletScoutListFragment.newInstance(args))
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
                it.containsKey(SCOUT_ARGS_KEY) -> onTeamSelected(it.getBundle(SCOUT_ARGS_KEY), true)
                it.containsKey(DONATE_EXTRA) -> DonateDialog.show(supportFragmentManager)
                it.containsKey(UPDATE_EXTRA) -> UpdateDialog.showStoreListing(this)
            }
        }

        intent.data?.let {
            if (it.toString() == ADD_SCOUT_INTENT) NewTeamDialog.show(supportFragmentManager)
        }

        // When the app is installed through a dynamic link, we can only get it from the launcher
        // activity so we have to check to see if there are any pending links and then forward those
        // along to the LinkReceiverActivity
        FirebaseDynamicLinks.getInstance()
                .getDynamicLink(intent)
                .addOnSuccessListener(this) {
                    val link = it?.link
                    if (link != null) startActivity(Intent(Intent.ACTION_VIEW, link))
                }
                .addOnFailureListener { FirebaseCrash.report(it) }
                .addOnFailureListener(this) {
                    Toast.makeText(this, R.string.uri_parse_error, Toast.LENGTH_LONG).show()
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

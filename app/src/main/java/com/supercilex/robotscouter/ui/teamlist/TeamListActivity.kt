package com.supercilex.robotscouter.ui.teamlist

import android.app.Activity
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.google.android.gms.auth.api.credentials.Credentials
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.supercilex.robotscouter.BuildConfig
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.client.LinkReceiverActivity
import com.supercilex.robotscouter.ui.scouting.scoutlist.ScoutListActivity
import com.supercilex.robotscouter.ui.scouting.templatelist.TemplateListActivity
import com.supercilex.robotscouter.ui.settings.SettingsActivity
import com.supercilex.robotscouter.util.CrashLogger
import com.supercilex.robotscouter.util.asLifecycleReference
import com.supercilex.robotscouter.util.data.SCOUT_ARGS_KEY
import com.supercilex.robotscouter.util.data.getTeam
import com.supercilex.robotscouter.util.fetchAndActivate
import com.supercilex.robotscouter.util.fullVersionCode
import com.supercilex.robotscouter.util.isOnline
import com.supercilex.robotscouter.util.isSignedIn
import com.supercilex.robotscouter.util.logFailures
import com.supercilex.robotscouter.util.logSelect
import com.supercilex.robotscouter.util.minimumAppVersion
import com.supercilex.robotscouter.util.ui.ActivityBase
import com.supercilex.robotscouter.util.ui.KeyboardShortcutHandler
import com.supercilex.robotscouter.util.ui.TeamSelectionListener
import com.supercilex.robotscouter.util.ui.isInTabletMode
import com.supercilex.robotscouter.util.ui.showAddTeamTutorial
import com.supercilex.robotscouter.util.ui.showSignInTutorial
import com.supercilex.robotscouter.util.unsafeLazy
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotterknife.bindView
import org.jetbrains.anko.find
import org.jetbrains.anko.longToast

class TeamListActivity : ActivityBase(), View.OnClickListener,
        NavigationView.OnNavigationItemSelectedListener, TeamSelectionListener {
    override val keyboardShortcutHandler = object : KeyboardShortcutHandler() {
        override fun onFilteredKeyUp(keyCode: Int, event: KeyEvent) {
            when (keyCode) {
                KeyEvent.KEYCODE_N -> if (event.isShiftPressed) {
                    scoutListFragment?.addScoutWithSelector()
                } else {
                    NewTeamDialog.show(supportFragmentManager)
                }
                KeyEvent.KEYCODE_E -> teamListFragment.exportTeams()
                KeyEvent.KEYCODE_D -> scoutListFragment?.showTeamDetails()
            }
        }
    }

    val teamListFragment by unsafeLazy {
        supportFragmentManager.findFragmentByTag(TeamListFragment.TAG) as TeamListFragment
    }
    val scoutListFragment
        get() = supportFragmentManager.findFragmentByTag(TabletScoutListFragment.TAG) as? TabletScoutListFragment
    private val authHelper by unsafeLazy { AuthHelper(this) }
    private val tutorialHelper: TutorialHelper by unsafeLazy {
        ViewModelProviders.of(this).get(TutorialHelper::class.java)
    }

    val drawerToggle: ActionBarDrawerToggle by unsafeLazy {
        ActionBarDrawerToggle(
                this,
                drawerLayout,
                find(R.id.toolbar),
                R.string.team_navigation_drawer_open_desc,
                R.string.team_navigation_drawer_close_desc
        )
    }
    private val drawerLayout: DrawerLayout by bindView(R.id.drawer_layout)

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.RobotScouter_NoActionBar_TransparentStatusBar)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_team_list)
        setSupportActionBar(find(R.id.toolbar))

        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()

        find<NavigationView>(R.id.drawer).setNavigationItemSelectedListener(this)

        find<View>(R.id.fab).setOnClickListener(this)
        showAddTeamTutorial(tutorialHelper.also { it.init(null) }, this)
        val ref = asLifecycleReference()
        async(UI) {
            authHelper // Force initialization on the main thread
            try {
                async { authHelper.init() }.await()
            } catch (e: Exception) {
                CrashLogger.onFailure(e)
                return@async
            }
            ref().handleIntent(ref().intent)
        }.logFailures()

        Credentials.getClient(this) // Ensure the Play Services update dialog is shown
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        val ref = asLifecycleReference()
        async(UI) {
            async { fetchAndActivate() }.await()
            if (!BuildConfig.DEBUG && fullVersionCode < minimumAppVersion && isOnline) {
                UpdateDialog.show(ref().supportFragmentManager)
            }
        }.logFailures()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.team_list_menu, menu)
        authHelper.initMenu(menu)
        showSignInTutorial(tutorialHelper, this)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = if (item.itemId == R.id.action_sign_in) {
        authHelper.signIn()
        true
    } else {
        false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        authHelper.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SCOUT && resultCode == Activity.RESULT_OK) {
            onTeamSelected(data!!.getBundleExtra(SCOUT_ARGS_KEY), true)
        }
    }

    override fun onClick(v: View) {
        if (v.id == R.id.fab) runIfSignedIn { NewTeamDialog.show(supportFragmentManager) }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_donate -> DonateDialog.show(supportFragmentManager)
            else -> runIfSignedIn {
                when (item.itemId) {
                    R.id.action_export_all_teams -> teamListFragment.exportTeams()
                    R.id.action_edit_templates -> startActivity(TemplateListActivity.createIntent())
                    R.id.action_settings -> SettingsActivity.show(this)
                    else -> error("Unknown item id: ${item.itemId}")
                }
            }
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
        args.getTeam().logSelect()

        if (isInTabletMode()) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.scout_list,
                             TabletScoutListFragment.newInstance(args),
                             TabletScoutListFragment.TAG)
                    .commit()
        } else {
            if (restoreOnConfigChange) {
                startActivityForResult(ScoutListActivity.createIntent(args), RC_SCOUT)
            } else {
                startActivity(ScoutListActivity.createIntent(args))
            }
        }
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
            if (it.toString() == ADD_SCOUT_INTENT) runIfSignedIn {
                NewTeamDialog.show(supportFragmentManager)
            }
            if (it.toString() == EXPORT_ALL_TEAMS_INTENT) runIfSignedIn {
                teamListFragment.exportTeams()
            }
        }

        // When the app is installed through a dynamic link, we can only get it from the launcher
        // activity so we have to check to see if there are any pending links and then forward those
        // along to the LinkReceiverActivity
        FirebaseDynamicLinks.getInstance().getDynamicLink(intent).addOnSuccessListener(this) {
            it?.link?.let {
                startActivity(Intent(this, LinkReceiverActivity::class.java).setData(it))
            }
        }.addOnFailureListener(this) {
            longToast(R.string.link_uri_parse_error)
        }.logFailures()

        this.intent = Intent() // Consume Intent
    }

    private inline fun runIfSignedIn(block: () -> Unit) =
            if (isSignedIn) block() else authHelper.showSignInResolution()

    private companion object {
        const val RC_SCOUT = 744

        const val DONATE_EXTRA = "donate_extra"
        const val UPDATE_EXTRA = "update_extra"
        const val ADD_SCOUT_INTENT = "add_scout"
        const val EXPORT_ALL_TEAMS_INTENT = "export_all_teams"
    }
}

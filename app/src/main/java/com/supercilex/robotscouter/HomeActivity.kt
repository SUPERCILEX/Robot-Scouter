package com.supercilex.robotscouter

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.Toolbar
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.supercilex.robotscouter.core.CrashLogger
import com.supercilex.robotscouter.core.asLifecycleReference
import com.supercilex.robotscouter.core.data.SCOUT_ARGS_KEY
import com.supercilex.robotscouter.core.data.fetchAndActivate
import com.supercilex.robotscouter.core.data.getTeam
import com.supercilex.robotscouter.core.data.ioPerms
import com.supercilex.robotscouter.core.data.isSignedIn
import com.supercilex.robotscouter.core.data.isTemplateEditingAllowed
import com.supercilex.robotscouter.core.data.logSelect
import com.supercilex.robotscouter.core.data.minimumAppVersion
import com.supercilex.robotscouter.core.data.waitForChange
import com.supercilex.robotscouter.core.fullVersionCode
import com.supercilex.robotscouter.core.isOnline
import com.supercilex.robotscouter.core.logFailures
import com.supercilex.robotscouter.core.ui.ActivityBase
import com.supercilex.robotscouter.core.ui.KeyboardShortcutHandler
import com.supercilex.robotscouter.core.ui.OnBackPressedListener
import com.supercilex.robotscouter.core.ui.TeamSelectionListener
import com.supercilex.robotscouter.core.ui.isInTabletMode
import com.supercilex.robotscouter.core.unsafeLazy
import com.supercilex.robotscouter.feature.exports.ExportService
import com.supercilex.robotscouter.feature.scouts.ScoutListActivity
import com.supercilex.robotscouter.feature.scouts.TabletScoutListFragment
import com.supercilex.robotscouter.feature.settings.SettingsActivity
import com.supercilex.robotscouter.feature.teams.BuildConfig
import com.supercilex.robotscouter.feature.teams.DrawerToggler
import com.supercilex.robotscouter.feature.teams.NewTeamDialog
import com.supercilex.robotscouter.feature.teams.SelectedTeamsRetriever
import com.supercilex.robotscouter.feature.teams.TeamExporter
import com.supercilex.robotscouter.feature.templates.TemplateListActivity
import com.supercilex.robotscouter.shared.PermissionRequestHandler
import com.supercilex.robotscouter.shared.UpdateDialog
import kotlinx.android.synthetic.main.activity_home_base.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import org.jetbrains.anko.find
import org.jetbrains.anko.longToast

class HomeActivity : ActivityBase(), View.OnClickListener,
        NavigationView.OnNavigationItemSelectedListener, TeamSelectionListener,
        DrawerToggler, TeamExporter {
    override val keyboardShortcutHandler = object : KeyboardShortcutHandler() {
        val scoutListFragment
            get() = supportFragmentManager.findFragmentByTag(TabletScoutListFragment.TAG) as? TabletScoutListFragment

        override fun onFilteredKeyUp(keyCode: Int, event: KeyEvent) {
            when (keyCode) {
                KeyEvent.KEYCODE_N -> if (event.isShiftPressed) {
                    scoutListFragment?.addScoutWithSelector()
                } else {
                    NewTeamDialog.show(supportFragmentManager)
                }
                KeyEvent.KEYCODE_E -> export()
                KeyEvent.KEYCODE_D -> scoutListFragment?.showTeamDetails()
            }
        }
    }

    private val authHelper by unsafeLazy { AuthHelper(this) }
    private val tutorialHelper: TutorialHelper by unsafeLazy {
        ViewModelProviders.of(this).get(TutorialHelper::class.java)
    }
    private val permHandler: PermissionRequestHandler by unsafeLazy {
        ViewModelProviders.of(this).get(PermissionRequestHandler::class.java)
    }

    private val toolbar by unsafeLazy { find<Toolbar>(R.id.toolbar) }
    private val drawerToggle by unsafeLazy {
        ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.navigation_drawer_open_desc,
                R.string.navigation_drawer_close_desc
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.RobotScouter_NoActionBar_TransparentStatusBar)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        setSupportActionBar(toolbar)

        permHandler.apply {
            init(ioPerms)
            onGranted.observe(this@HomeActivity, Observer { export() })
        }

        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()

        drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            private val editTemplates: MenuItem = drawer.menu.findItem(R.id.action_edit_templates)

            init {
                update()
            }

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) = update()

            private fun update() {
                editTemplates.isVisible = isTemplateEditingAllowed
            }
        })
        drawer.setNavigationItemSelectedListener(this)

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

        GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this)
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
        menuInflater.inflate(R.menu.home_menu, menu)
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

    override fun toggle(enabled: Boolean) {
        drawerToggle.isDrawerIndicatorEnabled = enabled
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        authHelper.onActivityResult(requestCode, resultCode, data)
        permHandler.onActivityResult(requestCode, resultCode, data)
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
                    R.id.action_export_all_teams -> export()
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
            forwardBack()
        }
    }

    override fun onTeamSelected(args: Bundle, restoreOnConfigChange: Boolean) {
        args.getTeam().logSelect()

        if (isInTabletMode()) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.scoutList,
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

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
    ) = permHandler.onRequestPermissionsResult(this, requestCode, permissions, grantResults)

    override fun export() {
        val selectedTeams = supportFragmentManager.fragments
                .filterIsInstance<SelectedTeamsRetriever>()
                .map { it.selectedTeams }
                .singleOrNull { it.isNotEmpty() }

        if (selectedTeams == null) {
            val ref = asLifecycleReference()
            async {
                val allTeams = com.supercilex.robotscouter.core.data.teams.waitForChange()
                ExportService.exportAndShareSpreadSheet(ref(), ref().permHandler, allTeams)
            }.logFailures()
        } else {
            if (ExportService.exportAndShareSpreadSheet(this, permHandler, selectedTeams)) {
                forwardBack()
            }
        }
    }

    private fun forwardBack() {
        val ignored = supportFragmentManager.fragments
                .filterIsInstance<OnBackPressedListener>()
                .none { it.onBackPressed() }
        if (ignored) super.onBackPressed()
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
            if (it.toString() == EXPORT_ALL_TEAMS_INTENT) runIfSignedIn { export() }
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

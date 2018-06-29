package com.supercilex.robotscouter

import android.app.Activity
import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
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
import com.supercilex.robotscouter.core.data.teams
import com.supercilex.robotscouter.core.data.waitForChange
import com.supercilex.robotscouter.core.fullVersionCode
import com.supercilex.robotscouter.core.isOnline
import com.supercilex.robotscouter.core.logFailures
import com.supercilex.robotscouter.core.ui.ActivityBase
import com.supercilex.robotscouter.core.ui.OnBackPressedListener
import com.supercilex.robotscouter.core.ui.TeamSelectionListener
import com.supercilex.robotscouter.core.ui.isInTabletMode
import com.supercilex.robotscouter.core.unsafeLazy
import com.supercilex.robotscouter.shared.PermissionRequestHandler
import com.supercilex.robotscouter.shared.UpdateDialog
import kotlinx.android.synthetic.main.activity_home_base.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import org.jetbrains.anko.find
import org.jetbrains.anko.longToast

internal class HomeActivity : ActivityBase(), NavigationView.OnNavigationItemSelectedListener,
        TeamSelectionListener, DrawerToggler, TeamExporter, SignInResolver {
    private val authHelper by unsafeLazy { AuthHelper(this) }
    private val permHandler: PermissionRequestHandler by unsafeLazy {
        ViewModelProviders.of(this).get(PermissionRequestHandler::class.java)
    }

    private var drawerToggle: ActionBarDrawerToggle? = null

    private val scoutListFragment
        get() = supportFragmentManager.findFragmentByTag(TabletScoutListFragmentCompanion.TAG)
                as TabletScoutListFragmentBridge?

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.RobotScouter_NoActionBar_TransparentStatusBar)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().add(
                    R.id.root,
                    TeamListFragmentCompanion().getInstance(supportFragmentManager),
                    TeamListFragmentCompanion.TAG
            ).commit()
        }
        supportFragmentManager.registerFragmentLifecycleCallbacks(object : FragmentManager
        .FragmentLifecycleCallbacks() {
            override fun onFragmentViewCreated(
                    fm: FragmentManager,
                    f: Fragment,
                    v: View,
                    savedInstanceState: Bundle?
            ) {
                if (
                    f.tag == TeamListFragmentCompanion.TAG ||
                    f.tag == AutoScoutFragmentCompanion.TAG
                ) initToolbar()
            }
        }, false)

        permHandler.apply {
            init(ioPerms)
            onGranted.observe(this@HomeActivity, Observer { export() })
        }

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
        bottomNavigation.setOnNavigationItemSelectedListener {
            supportFragmentManager.beginTransaction()
                    .detach(supportFragmentManager.findFragmentById(R.id.root))
                    .apply {
                        val (f, tag) = when (it.itemId) {
                            R.id.teams -> {
                                TeamListFragmentCompanion().getInstance(supportFragmentManager) to
                                        TeamListFragmentCompanion.TAG
                            }
                            R.id.autoScout -> {
                                AutoScoutFragmentCompanion().getInstance(supportFragmentManager) to
                                        AutoScoutFragmentCompanion.TAG
                            }
                            else -> error("Unknown id: ${it.itemId}")
                        }

                        if (f.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
                            attach(f)
                        } else {
                            add(R.id.root, f, tag)
                        }
                    }
                    .commit()

            true
        }

        val ref = asLifecycleReference()
        launch(UI) {
            authHelper // Force initialization on the main thread
            try {
                withContext(CommonPool) { authHelper.init() }
            } catch (e: Exception) {
                CrashLogger.onFailure(e)
                return@launch
            }
            ref().handleIntent(ref().intent)
        }

        GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        val ref = asLifecycleReference()
        launch(UI) {
            withContext(CommonPool) { fetchAndActivate() }
            if (!BuildConfig.DEBUG && fullVersionCode < minimumAppVersion && isOnline) {
                UpdateDialog.show(ref().supportFragmentManager)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.home_menu, menu)
        authHelper.initMenu(menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = if (item.itemId == R.id.action_sign_in) {
        authHelper.signIn()
        true
    } else {
        false
    }

    override fun toggle(enabled: Boolean) {
        checkNotNull(drawerToggle).isDrawerIndicatorEnabled = enabled
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        authHelper.onActivityResult(requestCode, resultCode, data)
        permHandler.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SCOUT && resultCode == Activity.RESULT_OK) {
            onTeamSelected(checkNotNull(data).getBundleExtra(SCOUT_ARGS_KEY), true)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_donate -> DonateDialog.show(supportFragmentManager)
            else -> runIfSignedIn {
                when (item.itemId) {
                    R.id.action_export_all_teams -> export()
                    R.id.action_edit_templates ->
                        startActivity(TemplateListActivityCompanion().createIntent())
                    R.id.action_settings -> SettingsActivityCompanion().show(this)
                    else -> error("Unknown item id: ${item.itemId}")
                }
            }
        }

        drawerLayout.closeDrawer(GravityCompat.START)
        return false
    }

    override fun onShortcut(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_N -> if (event.isShiftPressed) {
                scoutListFragment?.addScoutWithSelector()
            } else {
                return false
            }
            KeyEvent.KEYCODE_E -> export()
            KeyEvent.KEYCODE_D -> scoutListFragment?.showTeamDetails()
            else -> return false
        }
        return true
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
                             TabletScoutListFragmentCompanion().newInstance(args),
                             TabletScoutListFragmentCompanion.TAG)
                    .commit()
        } else {
            if (restoreOnConfigChange) {
                startActivityForResult(ScoutListActivityCompanion().createIntent(args), RC_SCOUT)
            } else {
                startActivity(ScoutListActivityCompanion().createIntent(args))
            }
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permHandler.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
    }

    override fun export() {
        val selectedTeams = supportFragmentManager.fragments
                .filterIsInstance<SelectedTeamsRetriever>()
                .map { it.selectedTeams }
                .singleOrNull { it.isNotEmpty() }

        if (selectedTeams == null) {
            val ref = asLifecycleReference()
            async {
                val allTeams = teams.waitForChange()
                ExportServiceCompanion()
                        .exportAndShareSpreadSheet(ref(), ref().permHandler, allTeams)
            }.logFailures()
        } else {
            if (
                ExportServiceCompanion().exportAndShareSpreadSheet(
                        this, permHandler, selectedTeams)
            ) forwardBack()
        }
    }

    private fun initToolbar() {
        drawerToggle?.let { drawerLayout.removeDrawerListener(it) }

        val toolbar = find<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar) // This must come first

        val drawerToggle = ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.navigation_drawer_open_desc,
                R.string.navigation_drawer_close_desc
        )
        this.drawerToggle = drawerToggle

        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()
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
                NewTeamDialogCompanion().show(supportFragmentManager)
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

    override fun showSignInResolution() {
        authHelper.showSignInResolution()
    }

    private inline fun runIfSignedIn(block: () -> Unit) =
            if (isSignedIn) block() else showSignInResolution()

    private companion object {
        const val RC_SCOUT = 744

        const val DONATE_EXTRA = "donate_extra"
        const val UPDATE_EXTRA = "update_extra"
        const val ADD_SCOUT_INTENT = "add_scout"
        const val EXPORT_ALL_TEAMS_INTENT = "export_all_teams"
    }
}

package com.supercilex.robotscouter

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.core.view.children
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.transaction
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.get
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.navigation.NavigationView
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.supercilex.robotscouter.core.data.SCOUT_ARGS_KEY
import com.supercilex.robotscouter.core.data.fetchAndActivateTask
import com.supercilex.robotscouter.core.data.getTeam
import com.supercilex.robotscouter.core.data.ioPerms
import com.supercilex.robotscouter.core.data.isSignedIn
import com.supercilex.robotscouter.core.data.isTemplateEditingAllowed
import com.supercilex.robotscouter.core.data.logSelect
import com.supercilex.robotscouter.core.data.minimumAppVersion
import com.supercilex.robotscouter.core.data.observeNonNull
import com.supercilex.robotscouter.core.fullVersionCode
import com.supercilex.robotscouter.core.isOnline
import com.supercilex.robotscouter.core.logFailures
import com.supercilex.robotscouter.core.model.Team
import com.supercilex.robotscouter.core.ui.ActivityBase
import com.supercilex.robotscouter.core.ui.OnBackPressedListener
import com.supercilex.robotscouter.core.ui.TeamSelectionListener
import com.supercilex.robotscouter.core.ui.isInTabletMode
import com.supercilex.robotscouter.core.ui.showStoreListing
import com.supercilex.robotscouter.core.unsafeLazy
import com.supercilex.robotscouter.shared.PermissionRequestHandler
import kotlinx.android.synthetic.main.activity_home_base.*
import kotlinx.android.synthetic.main.activity_home_content.*
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.longToast

internal class HomeActivity : ActivityBase(), NavigationView.OnNavigationItemSelectedListener,
        TeamSelectionListener, DrawerToggler, TeamExporter, SignInResolver {
    private val authHelper by unsafeLazy { AuthHelper(this) }
    private val permHandler by unsafeLazy {
        ViewModelProviders.of(this).get<PermissionRequestHandler>()
    }
    private val moduleRequestHolder by unsafeLazy {
        ViewModelProviders.of(this).get<ModuleRequestHolder>()
    }

    private val drawerToggle by unsafeLazy {
        ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.navigation_drawer_open_desc,
                R.string.navigation_drawer_close_desc
        )
    }

    private val scoutListFragment
        get() = supportFragmentManager.findFragmentByTag(TabletScoutListFragmentCompanion.TAG)
                as TabletScoutListFragmentBridge?

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.RobotScouter_NoActionBar_TransparentStatusBar)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        if (savedInstanceState == null) supportFragmentManager.transaction {
            add(R.id.content,
                TeamListFragmentCompanion().getInstance(supportFragmentManager),
                TeamListFragmentCompanion.TAG)
        }

        permHandler.apply {
            init(ioPerms)
            onGranted.observe(this@HomeActivity, Observer { export() })
        }
        moduleRequestHolder.onSuccess.observeNonNull(this) { (comp, args) ->
            @Suppress("UNCHECKED_CAST") // We know our inputs
            when (comp) {
                is TemplateListActivityCompanion -> startActivity(comp.createIntent())
                is SettingsActivityCompanion -> startActivity(comp.createIntent())
                is ExportServiceCompanion -> if (
                    comp.exportAndShareSpreadSheet(this, permHandler, args.single() as List<Team>)
                ) sendBackEventToChildren()
            }
        }

        setSupportActionBar(toolbar)
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
        bottomNavigation.setOnNavigationItemSelectedListener {
            supportFragmentManager.transaction {
                val currentFragment =
                        checkNotNull(supportFragmentManager.findFragmentById(R.id.content))
                val newTag = when (it.itemId) {
                    R.id.teams -> TeamListFragmentCompanion.TAG
                    R.id.autoScout -> AutoScoutFragmentCompanion.TAG
                    else -> error("Unknown id: ${it.itemId}")
                }

                if (currentFragment.tag == newTag) {
                    (currentFragment as? Refreshable)?.refresh()
                    return@transaction
                }

                val newFragment = when (newTag) {
                    TeamListFragmentCompanion.TAG ->
                        TeamListFragmentCompanion().getInstance(supportFragmentManager)
                    AutoScoutFragmentCompanion.TAG ->
                        AutoScoutFragmentCompanion().getInstance(supportFragmentManager)
                    else -> error("Unknown tag: $newTag")
                }

                setCustomAnimations(
                        R.anim.pop_fade_in,
                        R.anim.fade_out,
                        R.anim.pop_fade_in,
                        R.anim.fade_out
                )
                detach(currentFragment)
                if (newFragment.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
                    attach(newFragment)
                } else {
                    add(R.id.content, newFragment, newTag)
                }
            }

            true
        }

        handleModuleInstalls(moduleInstallProgress)
        authHelper.init().logFailures().addOnSuccessListener(this) {
            handleIntent(intent)
        }

        GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this)

        registerShortcut(KeyEvent.KEYCODE_N, KeyEvent.META_SHIFT_ON, 0) {
            scoutListFragment?.addScoutWithSelector()
        }
        registerShortcut(KeyEvent.KEYCODE_E, 0) { export() }
        registerShortcut(KeyEvent.KEYCODE_D, 0) { scoutListFragment?.showTeamDetails() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        fetchAndActivateTask().logFailures().addOnSuccessListener(this) {
            if (!BuildConfig.DEBUG && fullVersionCode < minimumAppVersion && isOnline) {
                UpdateDialog.show(supportFragmentManager)
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
                        moduleRequestHolder += TemplateListActivityCompanion().logFailures()
                    R.id.action_settings ->
                        moduleRequestHolder += SettingsActivityCompanion().logFailures()
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
        } else if (sendBackEventToChildren()) {
            val homeId = bottomNavigation.menu.children.first().itemId
            if (bottomNavigation.selectedItemId == homeId) {
                super.onBackPressed()
            } else {
                bottomNavigation.selectedItemId = homeId
            }
        }
    }

    override fun onTeamSelected(args: Bundle, restoreOnConfigChange: Boolean) {
        args.getTeam().logSelect()

        if (isInTabletMode()) {
            supportFragmentManager.transaction {
                replace(R.id.scoutList,
                        TabletScoutListFragmentCompanion().newInstance(args),
                        TabletScoutListFragmentCompanion.TAG)
            }
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
                .singleOrNull { it.isNotEmpty() } ?: emptyList()

        moduleRequestHolder += ExportServiceCompanion().logFailures() to listOf(selectedTeams)
    }

    private fun sendBackEventToChildren() = supportFragmentManager.fragments
            .filterIsInstance<OnBackPressedListener>()
            .none { it.onBackPressed() }

    private fun handleIntent(intent: Intent) {
        intent.extras?.let {
            when {
                it.containsKey(SCOUT_ARGS_KEY) -> onTeamSelected(it.getBundle(SCOUT_ARGS_KEY), true)
                it.containsKey(DONATE_EXTRA) -> DonateDialog.show(supportFragmentManager)
                it.containsKey(UPDATE_EXTRA) -> showStoreListing()
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
                startActivity(intentFor<LinkReceiverActivity>().setData(it))
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

package com.supercilex.robotscouter

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.core.view.children
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.transaction
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.get
import androidx.transition.TransitionInflater
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.navigation.NavigationView
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.supercilex.robotscouter.core.data.SCOUT_ARGS_KEY
import com.supercilex.robotscouter.core.data.TEMPLATE_ARGS_KEY
import com.supercilex.robotscouter.core.data.asLiveData
import com.supercilex.robotscouter.core.data.fetchAndActivateTask
import com.supercilex.robotscouter.core.data.getTeam
import com.supercilex.robotscouter.core.data.ioPerms
import com.supercilex.robotscouter.core.data.isSignedIn
import com.supercilex.robotscouter.core.data.isTemplateEditingAllowed
import com.supercilex.robotscouter.core.data.logSelect
import com.supercilex.robotscouter.core.data.minimumAppVersion
import com.supercilex.robotscouter.core.data.prefs
import com.supercilex.robotscouter.core.fullVersionCode
import com.supercilex.robotscouter.core.isOnline
import com.supercilex.robotscouter.core.logFailures
import com.supercilex.robotscouter.core.model.Team
import com.supercilex.robotscouter.core.ui.ActivityBase
import com.supercilex.robotscouter.core.ui.OnBackPressedListener
import com.supercilex.robotscouter.core.ui.isInTabletMode
import com.supercilex.robotscouter.core.ui.observeNonNull
import com.supercilex.robotscouter.core.ui.showStoreListing
import com.supercilex.robotscouter.core.unsafeLazy
import com.supercilex.robotscouter.shared.PermissionRequestHandler
import kotlinx.android.synthetic.main.activity_home_base.*
import kotlinx.android.synthetic.main.activity_home_content.*
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.itemsSequence
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
                is ExportServiceCompanion -> if (
                    comp.exportAndShareSpreadSheet(this, permHandler, args.single() as List<Team>)
                ) sendBackEventToChildren()
                is SettingsActivityCompanion -> startActivity(comp.createIntent())
            }
        }

        setSupportActionBar(toolbar)
        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()
        drawer.setNavigationItemSelectedListener(this)
        bottomNavigation.setOnNavigationItemSelectedListener listener@{
            val manager = supportFragmentManager

            val currentFragment = checkNotNull(manager.findFragmentById(R.id.content))
            val newTag = destIdToTag(it.itemId)

            if (currentFragment.tag == newTag) {
                (currentFragment as? Refreshable)?.refresh()
                return@listener true
            }
            if (
                it.itemId == R.id.teams &&
                currentFragment.tag === ScoutListFragmentCompanionBase.TAG
            ) {
                manager.popBackStack()
                return@listener true
            }

            manager.transaction {
                val newFragment = manager.destTagToFragment(newTag)

                setCustomAnimations(R.anim.pop_fade_in, R.anim.fade_out)
                detach(currentFragment)
                if (newFragment.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
                    attach(newFragment)
                } else {
                    add(R.id.content, newFragment, newTag)
                }
            }

            true
        }
        prefs.asLiveData().observe(this, Observer {
            bottomNavigation.menu.findItem(R.id.templates).isEnabled = isTemplateEditingAllowed
        })

        handleModuleInstalls(moduleInstallProgress)
        authHelper.init().logFailures().addOnSuccessListener(this) {
            handleIntent()
        }

        GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this)
    }

    private fun destIdToTag(id: Int) = when (id) {
        R.id.teams -> TeamListFragmentCompanion.TAG
        R.id.autoScout -> AutoScoutFragmentCompanion.TAG
        R.id.templates -> TemplateListFragmentCompanion.TAG
        else -> error("Unknown id: $id")
    }

    private fun FragmentManager.destTagToFragment(tag: String) = when (tag) {
        TeamListFragmentCompanion.TAG -> IntegratedScoutListFragmentCompanion().getInstance(this)
                ?: TeamListFragmentCompanion().getInstance(this)
        AutoScoutFragmentCompanion.TAG -> AutoScoutFragmentCompanion().getInstance(this)
        TemplateListFragmentCompanion.TAG -> TemplateListFragmentCompanion().getInstance(
                this, intent.extras?.getBundle(TEMPLATE_ARGS_KEY))
        else -> error("Unknown tag: $tag")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        this.intent = intent
        handleIntent()
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
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_donate -> DonateDialog.show(supportFragmentManager)
            else -> runIfSignedIn {
                when (item.itemId) {
                    R.id.action_export_all_teams -> export()
                    R.id.action_view_trash -> startActivity(TrashActivityCompanion().createIntent())
                    R.id.action_settings ->
                        moduleRequestHolder += SettingsActivityCompanion().logFailures()
                    else -> error("Unknown item id: ${item.itemId}")
                }
            }
        }

        drawerLayout.closeDrawer(GravityCompat.START)
        return false
    }

    override fun onShortcut(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_E -> export()
            else -> return false
        }
        return true
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

    override fun onTeamSelected(args: Bundle, transitionView: View?) {
        args.getTeam().logSelect()

        val manager = supportFragmentManager
        manager.transaction {
            val existing = IntegratedScoutListFragmentCompanion().getInstance(manager)?.also {
                manager.popBackStack()
            }

            if (isInTabletMode()) {
                setCustomAnimations(R.anim.pop_fade_in_right, R.anim.fade_out)
                replace(R.id.scoutList,
                        TabletScoutListFragmentCompanion().newInstance(args),
                        ScoutListFragmentCompanionBase.TAG)
            } else {
                val fragment = IntegratedScoutListFragmentCompanion().newInstance(args)

                setReorderingAllowed(true)
                if (bottomNavigation.selectedItemId != R.id.teams) {
                    bottomNavigation.selectedItemId = R.id.teams
                } else if (existing != null) {
                    setReorderingAllowed(false)
                } else if (
                    transitionView != null &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                ) {
                    addSharedElement(transitionView, "media")
                    fragment.sharedElementEnterTransition =
                            TransitionInflater.from(this@HomeActivity)
                                    .inflateTransition(android.R.transition.move)
                    fragment.sharedElementReturnTransition = null
                }
                setCustomAnimations(
                        R.anim.pop_fade_in,
                        R.anim.fade_out,
                        R.anim.fade_in,
                        R.anim.pop_fade_out
                )

                add(R.id.content, fragment, ScoutListFragmentCompanionBase.TAG)
                if (existing == null) detach(checkNotNull(manager.findFragmentById(R.id.content)))
                detach(TeamListFragmentCompanion().getInstance(manager))

                addToBackStack(null)
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

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level == TRIM_MEMORY_MODERATE || level == TRIM_MEMORY_RUNNING_LOW) {
            supportFragmentManager.apply {
                val currentDestId = bottomNavigation.selectedItemId
                val unfocusedFragments = bottomNavigation.menu.itemsSequence()
                        .filterNot { it.itemId == currentDestId }
                        .map { it.itemId }
                        .map(::destIdToTag)
                        .map(::findFragmentByTag)
                        .filterNotNull()
                        .toList()

                transaction { for (f in unfocusedFragments) remove(f) }
            }
        }
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

    private fun handleIntent() {
        intent.extras?.run {
            when {
                containsKey(SCOUT_ARGS_KEY) ->
                    onTeamSelected(checkNotNull(getBundle(SCOUT_ARGS_KEY)))
                containsKey(TEMPLATE_ARGS_KEY) -> {
                    val args = checkNotNull(getBundle(TEMPLATE_ARGS_KEY))
                    if (bottomNavigation.selectedItemId == R.id.templates) {
                        (TemplateListFragmentCompanion()
                                .getInstance(supportFragmentManager) as TemplateListFragmentBridge)
                                .handleArgs(args)
                    } else {
                        bottomNavigation.selectedItemId = R.id.templates
                    }
                }
                containsKey(DONATE_EXTRA) -> DonateDialog.show(supportFragmentManager)
                containsKey(UPDATE_EXTRA) -> showStoreListing()
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

        intent = Intent() // Consume Intent
    }

    override fun showSignInResolution() {
        authHelper.showSignInResolution()
    }

    private inline fun runIfSignedIn(block: () -> Unit) =
            if (isSignedIn) block() else showSignInResolution()

    private companion object {
        const val DONATE_EXTRA = "donate_extra"
        const val UPDATE_EXTRA = "update_extra"
        const val ADD_SCOUT_INTENT = "add_scout"
        const val EXPORT_ALL_TEAMS_INTENT = "export_all_teams"
    }
}

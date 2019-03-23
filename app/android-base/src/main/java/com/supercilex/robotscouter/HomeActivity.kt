package com.supercilex.robotscouter

import android.content.Intent
import android.graphics.Outline
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewOutlineProvider
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.net.toUri
import androidx.core.view.GravityCompat
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import androidx.fragment.app.commitNow
import androidx.lifecycle.observe
import androidx.transition.TransitionInflater
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.navigation.NavigationView
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.data.SCOUT_ARGS_KEY
import com.supercilex.robotscouter.core.data.TEMPLATE_ARGS_KEY
import com.supercilex.robotscouter.core.data.asLiveData
import com.supercilex.robotscouter.core.data.enableAutoScout
import com.supercilex.robotscouter.core.data.getTeam
import com.supercilex.robotscouter.core.data.ioPerms
import com.supercilex.robotscouter.core.data.isSignedIn
import com.supercilex.robotscouter.core.data.isTemplateEditingAllowed
import com.supercilex.robotscouter.core.data.logFailures
import com.supercilex.robotscouter.core.data.logSelect
import com.supercilex.robotscouter.core.data.minimumAppVersion
import com.supercilex.robotscouter.core.data.prefs
import com.supercilex.robotscouter.core.fullVersionCode
import com.supercilex.robotscouter.core.isOnline
import com.supercilex.robotscouter.core.model.Team
import com.supercilex.robotscouter.core.ui.ActivityBase
import com.supercilex.robotscouter.core.ui.isInTabletMode
import com.supercilex.robotscouter.core.ui.showStoreListing
import com.supercilex.robotscouter.core.ui.transitionAnimationDuration
import com.supercilex.robotscouter.core.unsafeLazy
import com.supercilex.robotscouter.shared.PermissionRequestHandler
import com.supercilex.robotscouter.shared.launchUrl
import kotlinx.android.synthetic.main.activity_home_base.*
import kotlinx.android.synthetic.main.activity_home_content.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.longToast

internal class HomeActivity : ActivityBase(), NavigationView.OnNavigationItemSelectedListener,
        TeamSelectionListener, DrawerToggler, TeamExporter, SignInResolver {
    private val authHelper by unsafeLazy { AuthHelper(this) }
    private val permHandler by viewModels<PermissionRequestHandler>()
    private val moduleRequestHolder by viewModels<ModuleRequestHolder>()

    private val drawerToggle by unsafeLazy {
        ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.navigation_drawer_open_desc,
                R.string.navigation_drawer_close_desc
        )
    }

    /**
     * Properly handling tablet mode changes is one of the hardest problems to solve in Robot
     * Scouter. The main issue arises from there being so many dependent fragment transactions and
     * simultaneous UI changes. These will be broken down here.
     *
     * ### Phone -> tablet
     *
     * 1. In the activity's super.onCreate, the [IntegratedScoutListFragmentCompanion]'s
     *    [Fragment.onCreate] will realize it's no longer valid and must be replaced by its
     *    [TabletScoutListFragmentCompanion] counterpart. The fragment will call [onTeamSelected]
     *    which will kick of a series of transactions.
     * 1. The back stack will be popped to remove the integrated fragment.
     * 1. The tablet fragment will replace anything in the [R.id.scoutList] container.
     * 1. The [R.id.content] container will be forced to be a [TeamListFragmentCompanion].
     * 1. The bottom nav will attempt to be updated to point to the teams fragment, but it will be
     *    null b/c we're still in [onCreate] and haven't called [setContentView] yet.
     * 1. This flag will be flipped which re-triggers a bottom nav update in [onStart].
     *
     * ### Tablet -> phone
     *
     * Everything is the same as above except for the fragment transactions.
     *
     * 1. The integrated fragment is still popped.
     * 1. A new integrated fragment is created and added while the teams fragment is detached. These
     *    transactions are now added to the back stack.
     * 1. Another transaction is created to remove any non-team fragments currently in the content
     *    container which isn't added to the back stack.
     */
    private var bottomNavStatusNeedsUpdatingHack = false

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.RobotScouter_NoActionBar_TransparentStatusBar)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                add(R.id.content,
                    TeamListFragmentCompanion().getInstance(supportFragmentManager),
                    TeamListFragmentCompanion.TAG)
            }
        } else if (bottomNavStatusNeedsUpdatingHack) {
            updateBottomNavStatusAfterTeamSelection()
        }

        permHandler.apply {
            init(ioPerms)
            onGranted.observe(this@HomeActivity) { export() }
        }
        moduleRequestHolder.onSuccess.observe(this) { (comp, args) ->
            @Suppress("UNCHECKED_CAST") // We know our inputs
            when (comp) {
                is ExportServiceCompanion -> if (
                    comp.exportAndShareSpreadSheet(this, permHandler, args.single() as List<Team>)
                ) sendBackEventToChildren()
            }
        }

        setSupportActionBar(toolbar)
        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()
        drawer.setNavigationItemSelectedListener(this)
        if (enableAutoScout) bottomNavigation.menu.findItem(R.id.autoScout).isVisible = true
        bottomNavigation.setOnNavigationItemSelectedListener listener@{
            if (bottomNavStatusNeedsUpdatingHack) return@listener true

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

            appBar.setExpanded(true)
            manager.commitNow {
                val newFragment = manager.destTagToFragment(newTag)

                setCustomAnimations(R.anim.pop_fade_in, R.anim.fade_out)
                detach(currentFragment)
                addOrAttachContent(newFragment, newTag)
            }

            true
        }
        prefs.asLiveData().observe(this) {
            bottomNavigation.menu.findItem(R.id.templates).isEnabled = isTemplateEditingAllowed
            if (isSignedIn) delegate.setLocalNightMode(AppCompatDelegate.getDefaultNightMode())
        }

        if (isInTabletMode() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            scoutList.outlineProvider = object : ViewOutlineProvider() {
                private val padding = resources.getDimensionPixelSize(R.dimen.spacing_mini)

                override fun getOutline(view: View, outline: Outline) {
                    // Without negative starting values, the outline will show up on top of the
                    // toolbar.
                    outline.setRect(-bottomNavigation.width - 100, -100, padding, view.height)
                }
            }
        }

        handleModuleInstalls(moduleInstallProgress)
        authHelper.init().logFailures("authInit").addOnSuccessListener(this) {
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

    private fun FragmentTransaction.addOrAttachContent(
            fragment: Fragment,
            tag: String
    ) = if (fragment.isDetached) {
        attach(fragment)
    } else {
        add(R.id.content, fragment, tag)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        this.intent = intent
        handleIntent()
    }

    override fun onStart() {
        super.onStart()
        if (!BuildConfig.DEBUG && fullVersionCode < minimumAppVersion && isOnline) {
            UpdateDialog.show(supportFragmentManager)
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
        when (val id = item.itemId) {
            R.id.action_donate -> DonateDialog.show(supportFragmentManager)
            R.id.action_donate_patreon ->
                launchUrl(this, "https://www.patreon.com/SUPERCILEX".toUri())
            else -> runIfSignedIn {
                when (id) {
                    R.id.action_export_all_teams -> export()
                    R.id.action_view_trash -> startActivity(TrashActivityCompanion().createIntent())
                    R.id.action_settings -> startActivity(SettingsActivityCompanion().createIntent())
                    else -> error("Unknown item id: $id")
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
        } else {
            val homeId = bottomNavigation.menu.children.first().itemId
            if (bottomNavigation.selectedItemId == homeId) {
                super.onBackPressed()
            } else {
                bottomNavigation.selectedItemId = homeId
            }
        }
    }

    /** @see bottomNavStatusNeedsUpdatingHack */
    override fun onTeamSelected(args: Bundle, transitionView: View?) {
        args.getTeam().logSelect()

        val manager = supportFragmentManager
        val existingScoutFragment = IntegratedScoutListFragmentCompanion().getInstance(manager)
        val existingContainerFragment = checkNotNull(manager.findFragmentById(R.id.content))

        if (existingScoutFragment != null) manager.popBackStack()

        if (isInTabletMode()) {
            manager.commit {
                setCustomAnimations(R.anim.pop_fade_in_right, R.anim.fade_out)
                replace(R.id.scoutList,
                        TabletScoutListFragmentCompanion().newInstance(args),
                        ScoutListFragmentCompanionBase.TAG)

                val teamsTag = TeamListFragmentCompanion.TAG
                if (existingContainerFragment.tag != teamsTag) {
                    addOrAttachContent(TeamListFragmentCompanion().getInstance(manager), teamsTag)
                    detach(existingContainerFragment)
                }
            }
        } else {
            manager.commit {
                val fragment = IntegratedScoutListFragmentCompanion().newInstance(args)

                setReorderingAllowed(true)
                if (existingScoutFragment != null) {
                    setReorderingAllowed(false)
                } else if (
                    transitionView != null &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                ) {
                    addSharedElement(transitionView, "media")
                    fragment.sharedElementEnterTransition = fragmentTransition.clone()
                    fragment.sharedElementReturnTransition = null
                }
                setCustomAnimations(
                        R.anim.pop_fade_in,
                        R.anim.fade_out,
                        R.anim.fade_in,
                        R.anim.pop_fade_out
                )

                add(R.id.content, fragment, ScoutListFragmentCompanionBase.TAG)
                detach(TeamListFragmentCompanion().getInstance(manager))

                addToBackStack(null)
            }

            // Don't include detaching non-team fragments in the back stack
            if (
                existingScoutFragment == null &&
                existingContainerFragment.tag != TeamListFragmentCompanion.TAG
            ) manager.commit { detach(existingContainerFragment) }
        }

        updateBottomNavStatusAfterTeamSelection()
    }

    /** @see bottomNavStatusNeedsUpdatingHack */
    private fun updateBottomNavStatusAfterTeamSelection() {
        bottomNavStatusNeedsUpdatingHack = true

        val nav = bottomNavigation ?: return
        nav.post {
            nav.selectedItemId = R.id.teams
            bottomNavStatusNeedsUpdatingHack = false
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
                val unfocusedFragments = bottomNavigation.menu.children
                        .filterNot { it.itemId == currentDestId }
                        .map(MenuItem::getItemId)
                        .map(::destIdToTag)
                        .map(::findFragmentByTag)
                        .filterNotNull()
                        .toList()

                commit(allowStateLoss = true) { for (f in unfocusedFragments) remove(f) }
            }
        }
    }

    override fun export() {
        val selectedTeams = supportFragmentManager.fragments
                .filterIsInstance<SelectedTeamsRetriever>()
                .map { it.selectedTeams }
                .singleOrNull { it.isNotEmpty() } ?: emptyList()

        moduleRequestHolder += ExportServiceCompanion().logFailures("downloadExportModule") to
                listOf(selectedTeams)
    }

    private fun sendBackEventToChildren() = supportFragmentManager.fragments
            .filterIsInstance<OnBackPressedCallback>()
            .none { it.handleOnBackPressed() }

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
        }.logFailures("getDynamicLink")

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

        val fragmentTransition by unsafeLazy {
            TransitionInflater.from(RobotScouter)
                    .inflateTransition(android.R.transition.move)
                    .setDuration(transitionAnimationDuration)
        }

        init {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                GlobalScope.launch { fragmentTransition }
            }
        }
    }
}

package com.supercilex.robotscouter.feature.scouts

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.appindexing.FirebaseUserActions
import com.supercilex.robotscouter.core.data.KEY_ADD_SCOUT
import com.supercilex.robotscouter.core.data.KEY_OVERRIDE_TEMPLATE_KEY
import com.supercilex.robotscouter.core.data.TEMPLATE_ARGS_KEY
import com.supercilex.robotscouter.core.data.getScoutBundle
import com.supercilex.robotscouter.core.data.getTabId
import com.supercilex.robotscouter.core.data.getTabIdBundle
import com.supercilex.robotscouter.core.data.getTeam
import com.supercilex.robotscouter.core.data.logFailures
import com.supercilex.robotscouter.core.data.model.TeamHolder
import com.supercilex.robotscouter.core.data.model.addScout
import com.supercilex.robotscouter.core.data.model.copyMediaInfo
import com.supercilex.robotscouter.core.data.model.forceUpdate
import com.supercilex.robotscouter.core.data.model.ownsTemplateTask
import com.supercilex.robotscouter.core.data.model.processPotentialMediaUpload
import com.supercilex.robotscouter.core.data.viewAction
import com.supercilex.robotscouter.core.isOffline
import com.supercilex.robotscouter.core.model.Team
import com.supercilex.robotscouter.core.model.TemplateType
import com.supercilex.robotscouter.core.ui.FragmentBase
import com.supercilex.robotscouter.core.ui.KeyboardShortcutListener
import com.supercilex.robotscouter.core.ui.LifecycleAwareLazy
import com.supercilex.robotscouter.core.ui.RecyclerPoolHolder
import com.supercilex.robotscouter.core.ui.hasPermsOnRequestPermissionsResult
import com.supercilex.robotscouter.core.ui.longSnackbar
import com.supercilex.robotscouter.core.ui.requestPerms
import com.supercilex.robotscouter.core.unsafeLazy
import com.supercilex.robotscouter.home
import com.supercilex.robotscouter.shared.ShouldUploadMediaToTbaDialog
import com.supercilex.robotscouter.shared.TeamDetailsDialog
import com.supercilex.robotscouter.shared.TeamMediaCreator
import com.supercilex.robotscouter.shared.TeamSharer
import kotlinx.android.synthetic.main.fragment_scout_list.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

internal abstract class ScoutListFragmentBase : FragmentBase(R.layout.fragment_scout_list),
        RecyclerPoolHolder, TemplateSelectionListener, Observer<Team?>,
        KeyboardShortcutListener {
    override val recyclerPool by LifecycleAwareLazy { RecyclerView.RecycledViewPool() }

    protected var viewHolder: AppBarViewHolderBase by LifecycleAwareLazy()
        private set

    private val mediaCreator by viewModels<TeamMediaCreator>()

    protected val dataHolder by viewModels<TeamHolder>()
    private lateinit var team: Team
    // It's not a lateinit because it could be used before initialization
    var pagerAdapter: ScoutPagerAdapter? = null
        private set

    private var savedState: Bundle? = null

    private val tabs: TabLayout by LifecycleAwareLazy {
        view?.findViewById(R.id.tabs) ?: requireActivity().findViewById(R.id.tabs)
    }

    private val scoutId: String?
        get() {
            val savedState = savedState
            var scoutId: String? = pagerAdapter?.currentTabId
            if (scoutId == null && savedState != null) scoutId = getTabId(savedState)
            if (scoutId == null) scoutId = getTabId(arguments)
            return scoutId
        }
    protected val bundle: Bundle
        get() = getScoutBundle(
                team, requireArguments().getBoolean(KEY_ADD_SCOUT), scoutId = scoutId)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        savedState = savedInstanceState

        team = requireArguments().getTeam()
        dataHolder.init(team)
        dataHolder.teamListener.observe(this, this)
    }

    override fun onChanged(team: Team?) {
        if (team == null) {
            onTeamDeleted()
        } else {
            this.team = team
            if (pagerAdapter == null) initScoutList()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (savedInstanceState == null && isOffline) {
            view.longSnackbar(R.string.scout_offline_rationale)
        }

        if (pagerAdapter != null) {
            viewPager.adapter = pagerAdapter
            tabs.setupWithViewPager(viewPager)
        }

        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            mediaCreator.viewActions.collect { onViewActionRequested(it) }
        }
        mediaCreator.state.observe(viewLifecycleOwner) {
            onViewStateChanged(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewPager.adapter = null
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewHolder = newViewModel()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        viewHolder.initMenu()
    }

    override fun onStart() {
        super.onStart()
        FirebaseUserActions.getInstance().start(team.viewAction).logFailures("startScoutingAction")
    }

    override fun onStop() {
        super.onStop()
        FirebaseUserActions.getInstance().end(team.viewAction).logFailures("endScoutingAction")
    }

    override fun onSaveInstanceState(outState: Bundle) = outState.putAll(getTabIdBundle(scoutId))

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
    ) {
        if (hasPermsOnRequestPermissionsResult(requestCode, permissions, grantResults)) {
            mediaCreator.capture()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        mediaCreator.onActivityResult(requestCode, resultCode, data)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_new_scout -> addScout()
            R.id.action_add_media -> mediaCreator.capture()
            R.id.action_share -> TeamSharer.shareTeams(this, listOf(team))
            R.id.action_edit_template -> {
                val templateId = team.templateId
                val intent = requireContext().home()
                        .putExtra(TEMPLATE_ARGS_KEY, getTabIdBundle(templateId))

                TemplateType.coerce(templateId)?.let {
                    startActivity(intent)
                    return true
                }

                ownsTemplateTask(templateId).addOnSuccessListener(requireActivity()) {
                    if (it) {
                        startActivity(intent)
                    } else if (view != null) {
                        tabs.longSnackbar(R.string.scout_template_access_denied_error)
                    }
                }
            }
            R.id.action_edit_team_details -> showTeamDetails()
            else -> return false
        }
        return true
    }

    override fun onShortcut(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_N -> if (event.isShiftPressed) {
                addScoutWithSelector()
            } else {
                addScout()
            }
            KeyEvent.KEYCODE_D -> showTeamDetails()
            else -> return false
        }
        return true
    }

    private fun onViewActionRequested(action: TeamMediaCreator.ViewAction) {
        when (action) {
            is TeamMediaCreator.ViewAction.RequestPermissions ->
                requestPerms(action.perms.toTypedArray(), action.rationaleId)
            is TeamMediaCreator.ViewAction.StartIntentForResult ->
                startActivityForResult(action.intent, action.rc)
            is TeamMediaCreator.ViewAction.ShowTbaUploadDialog ->
                ShouldUploadMediaToTbaDialog.show(childFragmentManager)
        }
    }

    private fun onViewStateChanged(state: TeamMediaCreator.State) {
        val image = state.image
        if (image != null) {
            mediaCreator.reset()
            GlobalScope.launch {
                val team = team.copy()
                team.copyMediaInfo(image.media, image.shouldUploadMediaToTba)
                team.processPotentialMediaUpload()
                team.forceUpdate(true)
            }
        }
    }

    private fun showTeamDetails() = TeamDetailsDialog.show(childFragmentManager, team)

    private fun addScoutWithSelector() = ScoutTemplateSelectorDialog.show(childFragmentManager)

    private fun addScout(id: String? = null) {
        checkNotNull(pagerAdapter).apply {
            currentTabId = team.addScout(id, holder.scouts)
        }
    }

    override fun onTemplateSelected(id: String) = addScout(id)

    private fun initScoutList() {
        pagerAdapter = ScoutPagerAdapter(this, team)
        checkNotNull(pagerAdapter).currentTabId = scoutId

        viewPager.adapter = pagerAdapter
        tabs.setupWithViewPager(viewPager)

        requireArguments().let {
            if (it.getBoolean(KEY_ADD_SCOUT, false)) {
                it.remove(KEY_ADD_SCOUT)
                addScout(it.getString(KEY_OVERRIDE_TEMPLATE_KEY, null))
            }
        }
    }

    protected abstract fun newViewModel(): AppBarViewHolderBase

    protected abstract fun onTeamDeleted()

    protected companion object {
        private val performOptionsItemSelectedField by unsafeLazy {
            Fragment::class.java
                    .getDeclaredMethod("performOptionsItemSelected", MenuItem::class.java)
                    .apply { isAccessible = true }
        }

        fun Fragment.forceRecursiveMenuItemSelection(item: MenuItem) =
                performOptionsItemSelectedField.invoke(this, item) as Boolean
    }
}

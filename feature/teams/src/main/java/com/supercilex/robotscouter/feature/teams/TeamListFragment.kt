package com.supercilex.robotscouter.feature.teams

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.get
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.supercilex.robotscouter.Bridge
import com.supercilex.robotscouter.Refreshable
import com.supercilex.robotscouter.SelectedTeamsRetriever
import com.supercilex.robotscouter.SignInResolver
import com.supercilex.robotscouter.TeamListFragmentCompanion
import com.supercilex.robotscouter.TeamListFragmentCompanion.Companion.TAG
import com.supercilex.robotscouter.core.data.TEAM_KEY
import com.supercilex.robotscouter.core.data.asLiveData
import com.supercilex.robotscouter.core.data.isSignedIn
import com.supercilex.robotscouter.core.data.observeNonNull
import com.supercilex.robotscouter.core.data.teams
import com.supercilex.robotscouter.core.ui.FragmentBase
import com.supercilex.robotscouter.core.ui.KeyboardShortcutListener
import com.supercilex.robotscouter.core.ui.OnBackPressedListener
import com.supercilex.robotscouter.core.ui.TeamSelectionListener
import com.supercilex.robotscouter.core.ui.animatePopReveal
import com.supercilex.robotscouter.core.unsafeLazy
import kotlinx.android.synthetic.main.fragment_team_list.*
import org.jetbrains.anko.find
import com.supercilex.robotscouter.R as RC

@Bridge
internal class TeamListFragment : FragmentBase(), TeamSelectionListener, SelectedTeamsRetriever,
        OnBackPressedListener, KeyboardShortcutListener, Refreshable, View.OnClickListener {
    override val selectedTeams get() = menuHelper.selectedTeams

    private val holder by unsafeLazy {
        ViewModelProviders.of(this).get<TeamListHolder>()
                .also { onHolderReadyTask.setResult(it) }
    }
    private val onHolderReadyTask = TaskCompletionSource<TeamListHolder>()
    private val tutorialHelper by unsafeLazy {
        ViewModelProviders.of(this).get<TutorialHelper>()
    }

    private val fab by unsafeLazy { requireActivity().find<FloatingActionButton>(RC.id.fab) }
    private lateinit var adapter: TeamListAdapter
    private lateinit var menuHelper: TeamMenuHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        holder.init(savedInstanceState)
        tutorialHelper.init(null)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View = View.inflate(context, R.layout.fragment_team_list, null)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        fab.setOnClickListener(this)
        fab.show()
        showAddTeamTutorial(tutorialHelper, this)

        teamsView.layoutManager = LinearLayoutManager(context)
        teamsView.setHasFixedSize(true)
        teamsView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0) {
                    // User scrolled down -> hide the FAB
                    fab.hide()
                } else if (dy < 0 && menuHelper.selectedTeams.isEmpty()) {
                    fab.show()
                }
            }
        })

        menuHelper = TeamMenuHelper(this, teamsView, run {
            if (::menuHelper.isInitialized) menuHelper.selectedTeams else emptyList()
        })
        adapter = TeamListAdapter(
                savedInstanceState,
                this,
                menuHelper,
                holder.selectedTeamIdListener
        )
        teamsView.adapter = adapter
        menuHelper.adapter = adapter
        menuHelper.restoreState(savedInstanceState)

        teams.asLiveData().observeNonNull(viewLifecycleOwner) {
            val noTeams = it.isEmpty()
            noTeamsHint.animatePopReveal(noTeams)
            if (noTeams) fab.show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        menuHelper.resetToolbarWithSave()
        fab.hide()
        fab.isVisible = false // Hack: don't animate
    }

    override fun onSaveInstanceState(outState: Bundle) {
        holder.onSaveInstanceState(outState)
        if (::menuHelper.isInitialized) menuHelper.saveState(outState)
        if (::adapter.isInitialized) adapter.onSaveInstanceState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menuHelper.onCreateOptionsMenu(menu, inflater)
        if (menuHelper.selectedTeams.isEmpty()) fab.show()
        adapter.startScroll()
        showSignInTutorial(tutorialHelper, this)
    }

    override fun onOptionsItemSelected(item: MenuItem) = menuHelper.onOptionsItemSelected(item)

    override fun onTeamSelected(args: Bundle, restoreOnConfigChange: Boolean) {
        onHolderReadyTask.task.addOnSuccessListener { it.selectTeam(args.getParcelable(TEAM_KEY)) }
    }

    override fun onClick(v: View) {
        if (isSignedIn) {
            NewTeamDialog.show(childFragmentManager)
        } else {
            (activity as SignInResolver).showSignInResolution()
        }
    }

    override fun onShortcut(keyCode: Int, event: KeyEvent) = if (keyCode == KeyEvent.KEYCODE_N) {
        onClick(fab)
        true
    } else {
        false
    }

    override fun refresh() {
        menuHelper.resetMenu()
        teamsView.smoothScrollToPosition(0)
    }

    override fun onBackPressed(): Boolean = menuHelper.onBackPressed()

    companion object : TeamListFragmentCompanion {
        override fun getInstance(manager: FragmentManager) =
                manager.findFragmentByTag(TAG) as TeamListFragment? ?: TeamListFragment()
    }
}

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
import androidx.recyclerview.selection.MutableSelection
import androidx.recyclerview.selection.Selection
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.supercilex.robotscouter.Bridge
import com.supercilex.robotscouter.Refreshable
import com.supercilex.robotscouter.SelectedTeamsRetriever
import com.supercilex.robotscouter.SignInResolver
import com.supercilex.robotscouter.TeamListFragmentCompanion
import com.supercilex.robotscouter.TeamListFragmentCompanion.Companion.TAG
import com.supercilex.robotscouter.TeamSelectionListener
import com.supercilex.robotscouter.common.FIRESTORE_TEAMS
import com.supercilex.robotscouter.core.data.TEAM_KEY
import com.supercilex.robotscouter.core.data.asLiveData
import com.supercilex.robotscouter.core.data.isSignedIn
import com.supercilex.robotscouter.core.data.teams
import com.supercilex.robotscouter.core.model.Team
import com.supercilex.robotscouter.core.ui.AllChangesSelectionObserver
import com.supercilex.robotscouter.core.ui.FragmentBase
import com.supercilex.robotscouter.core.ui.KeyboardShortcutListener
import com.supercilex.robotscouter.core.ui.LifecycleAwareLazy
import com.supercilex.robotscouter.core.ui.OnBackPressedListener
import com.supercilex.robotscouter.core.ui.animatePopReveal
import com.supercilex.robotscouter.core.ui.isInTabletMode
import com.supercilex.robotscouter.core.ui.notifyItemsNoChangeAnimation
import com.supercilex.robotscouter.core.ui.observeNonNull
import com.supercilex.robotscouter.core.ui.onDestroy
import com.supercilex.robotscouter.core.unsafeLazy
import kotlinx.android.synthetic.main.fragment_team_list.*
import org.jetbrains.anko.find
import com.supercilex.robotscouter.R as RC

@Bridge
internal class TeamListFragment : FragmentBase(), TeamSelectionListener, SelectedTeamsRetriever,
        OnBackPressedListener, KeyboardShortcutListener, Refreshable, View.OnClickListener {
    override val selectedTeams: List<Team>
        get() = if (view == null) {
            emptyList()
        } else {
            selectionTracker.selection.map { id -> adapter.snapshots.first { it.id == id } }
        }

    private val holder by unsafeLazy {
        ViewModelProviders.of(this).get<TeamListHolder>()
    }
    private val tutorialHelper by unsafeLazy {
        ViewModelProviders.of(this).get<TutorialHelper>()
    }

    private val fab by unsafeLazy { requireActivity().find<FloatingActionButton>(RC.id.fab) }
    private var adapter: TeamListAdapter by LifecycleAwareLazy()
    private var selectionTracker by LifecycleAwareLazy<SelectionTracker<String>>() onDestroy {
        savedSelection = MutableSelection<String>().apply { it.copySelection(this) }
        it.clearSelection() // Reset menu
    }
    private var menuHelper: TeamMenuHelper by LifecycleAwareLazy()
    private var savedSelection: Selection<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        holder.init(savedInstanceState)
        tutorialHelper.init(null)

        if (!requireContext().isInTabletMode()) holder.selectTeam(null)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_team_list, container, false)

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
                } else if (dy < 0 && !selectionTracker.hasSelection()) {
                    fab.show()
                }
            }
        })

        adapter = TeamListAdapter(
                savedInstanceState,
                this,
                holder.selectedTeamIdListener
        )
        teamsView.adapter = adapter

        selectionTracker = run {
            SelectionTracker.Builder<String>(
                    FIRESTORE_TEAMS,
                    teamsView,
                    TeamKeyProvider(adapter),
                    TeamDetailsLookup(teamsView),
                    StorageStrategy.createStringStorage()
            ).build().apply {
                addObserver(TeamMenuHelper(this@TeamListFragment, this)
                                    .also { menuHelper = it })
                addObserver(SnackbarSelectionObserver(view, this, adapter.snapshots))
                addObserver(object : AllChangesSelectionObserver<String>() {
                    override fun onItemStateChanged(key: String, selected: Boolean) {
                        if (selection.size() <= 1) { // First item added or last one removed
                            teamsView.notifyItemsNoChangeAnimation(
                                    // Prevent recursive loop
                                    SelectionTracker.SELECTION_CHANGED_MARKER)
                        }
                    }
                })

                val savedSelection = savedSelection
                if (savedSelection == null) {
                    onRestoreInstanceState(savedInstanceState)
                } else {
                    setItemsSelected(savedSelection, true)
                }
            }
        }
        adapter.selectionTracker = selectionTracker

        teams.asLiveData().observeNonNull(viewLifecycleOwner) {
            val noTeams = it.isEmpty()
            noTeamsHint.animatePopReveal(noTeams)
            if (noTeams) fab.show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fab.hide()
        fab.isVisible = false // TODO hack: don't animate
    }

    override fun onSaveInstanceState(outState: Bundle) {
        holder.onSaveInstanceState(outState)
        if (view != null) {
            adapter.onSaveInstanceState(outState)
            selectionTracker.onSaveInstanceState(outState)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menuHelper.onCreateOptionsMenu(menu, inflater)
        adapter.startScroll()
        showSignInTutorial(tutorialHelper, this)
    }

    override fun onOptionsItemSelected(item: MenuItem) = menuHelper.onOptionsItemSelected(item)

    override fun onTeamSelected(args: Bundle, transitionView: View?) {
        holder.selectTeam(args.getParcelable(TEAM_KEY))
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
        selectionTracker.clearSelection()
        teamsView.smoothScrollToPosition(0)
    }

    override fun onBackPressed() = selectionTracker.clearSelection()

    companion object : TeamListFragmentCompanion {
        override fun getInstance(manager: FragmentManager) =
                manager.findFragmentByTag(TAG) as TeamListFragment? ?: TeamListFragment()
    }
}

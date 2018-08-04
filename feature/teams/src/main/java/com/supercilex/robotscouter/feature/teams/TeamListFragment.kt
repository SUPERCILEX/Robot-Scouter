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
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.supercilex.robotscouter.Bridge
import com.supercilex.robotscouter.Refreshable
import com.supercilex.robotscouter.SelectedTeamsRetriever
import com.supercilex.robotscouter.SignInResolver
import com.supercilex.robotscouter.TeamListFragmentCompanion
import com.supercilex.robotscouter.TeamListFragmentCompanion.Companion.TAG
import com.supercilex.robotscouter.common.FIRESTORE_TEAMS
import com.supercilex.robotscouter.core.data.TEAM_KEY
import com.supercilex.robotscouter.core.data.asLiveData
import com.supercilex.robotscouter.core.data.isSignedIn
import com.supercilex.robotscouter.core.data.observeNonNull
import com.supercilex.robotscouter.core.data.teams
import com.supercilex.robotscouter.core.model.Team
import com.supercilex.robotscouter.core.ui.FragmentBase
import com.supercilex.robotscouter.core.ui.KeyboardShortcutListener
import com.supercilex.robotscouter.core.ui.OnBackPressedListener
import com.supercilex.robotscouter.core.ui.TeamSelectionListener
import com.supercilex.robotscouter.core.ui.animatePopReveal
import com.supercilex.robotscouter.core.ui.notifyItemsNoChangeAnimation
import com.supercilex.robotscouter.core.unsafeLazy
import kotlinx.android.synthetic.main.fragment_team_list.*
import org.jetbrains.anko.find
import com.supercilex.robotscouter.R as RC

@Bridge
internal class TeamListFragment : FragmentBase(), TeamSelectionListener, SelectedTeamsRetriever,
        OnBackPressedListener, KeyboardShortcutListener, Refreshable, View.OnClickListener {
    override val selectedTeams: List<Team>
        get() = if (::adapter.isInitialized) {
            selectionTracker.selection.map { id -> adapter.snapshots.first { it.id == id } }
        } else {
            emptyList()
        }

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
    private lateinit var selectionTracker: SelectionTracker<String>
    private lateinit var menuHelper: TeamMenuHelper
    private var savedSelection: Selection<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        holder.init(savedInstanceState)
        tutorialHelper.init(null)

        registerShortcut(KeyEvent.KEYCODE_N, 0) { onClick(fab) }
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
        savedSelection = MutableSelection<String>().apply { selectionTracker.copySelection(this) }
        selectionTracker.clearSelection()

        fab.hide()
        fab.isVisible = false // Hack: don't animate
    }

    override fun onSaveInstanceState(outState: Bundle) {
        holder.onSaveInstanceState(outState)
        if (::adapter.isInitialized) adapter.onSaveInstanceState(outState)
        if (::selectionTracker.isInitialized) selectionTracker.onSaveInstanceState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menuHelper.onCreateOptionsMenu(menu, inflater)
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

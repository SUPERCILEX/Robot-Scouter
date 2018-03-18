package com.supercilex.robotscouter.ui.teamlist

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.tasks.TaskCompletionSource
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.client.spreadsheet.ExportService
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.asLifecycleReference
import com.supercilex.robotscouter.util.data.asLiveData
import com.supercilex.robotscouter.util.data.ioPerms
import com.supercilex.robotscouter.util.data.teams
import com.supercilex.robotscouter.util.data.waitForChange
import com.supercilex.robotscouter.util.logFailures
import com.supercilex.robotscouter.util.ui.FragmentBase
import com.supercilex.robotscouter.util.ui.OnBackPressedListener
import com.supercilex.robotscouter.util.ui.PermissionRequestHandler
import com.supercilex.robotscouter.util.ui.animatePopReveal
import com.supercilex.robotscouter.util.unsafeLazy
import kotlinx.android.synthetic.main.fragment_team_list.*
import kotlinx.coroutines.experimental.async
import org.jetbrains.anko.find

class TeamListFragment : FragmentBase(), OnBackPressedListener {
    private val holder: TeamListHolder by unsafeLazy {
        ViewModelProviders.of(this).get(TeamListHolder::class.java)
                .also { onHolderReadyTask.setResult(it) }
    }
    private val onHolderReadyTask = TaskCompletionSource<TeamListHolder>()

    private lateinit var adapter: TeamListAdapter
    private val menuHelper by unsafeLazy { TeamMenuHelper(this, teamsView) }
    private val permHandler: PermissionRequestHandler by unsafeLazy {
        ViewModelProviders.of(this).get(PermissionRequestHandler::class.java)
    }

    private val fab: FloatingActionButton by unsafeLazy {
        requireActivity().find<FloatingActionButton>(R.id.fab)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        holder.init(savedInstanceState)
        permHandler.apply {
            init(ioPerms)
            onGranted.observe(this@TeamListFragment, Observer { exportTeams() })
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? = View.inflate(context, R.layout.fragment_team_list, null)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        teamsView.layoutManager = LinearLayoutManager(context)
        teamsView.setHasFixedSize(true)
        teamsView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                if (dy > 0) {
                    // User scrolled down -> hide the FAB
                    fab.hide()
                } else if (dy < 0 && menuHelper.selectedTeams.isEmpty()) {
                    fab.show()
                }
            }
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

        teams.asLiveData().observe(this, Observer {
            val noTeams = it!!.isEmpty()
            noTeamsHint.animatePopReveal(noTeams)
            if (noTeams) fab.show()
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        holder.onSaveInstanceState(outState)
        menuHelper.saveState(outState)
        adapter.onSaveInstanceState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menuHelper.onCreateOptionsMenu(menu, inflater)
        if (menuHelper.selectedTeams.isEmpty()) fab.show()
        adapter.startScroll()
    }

    override fun onOptionsItemSelected(item: MenuItem) = menuHelper.onOptionsItemSelected(item)

    fun selectTeam(team: Team?) = onHolderReadyTask.task.addOnSuccessListener {
        it.selectTeam(team)
    }

    override fun onBackPressed(): Boolean = menuHelper.onBackPressed()

    fun resetMenu() = menuHelper.resetMenu()

    fun exportTeams() {
        val selectedTeams = menuHelper.selectedTeams
        if (selectedTeams.isEmpty()) {
            val ref = asLifecycleReference()
            async {
                val allTeams = teams.waitForChange()
                ExportService.exportAndShareSpreadSheet(ref(), ref().permHandler, allTeams)
            }.logFailures()
        } else {
            if (ExportService.exportAndShareSpreadSheet(this, permHandler, selectedTeams)) {
                resetMenu()
            }
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
    ) = permHandler.onRequestPermissionsResult(this, requestCode, permissions, grantResults)

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) =
            permHandler.onActivityResult(requestCode, resultCode, data)

    companion object {
        const val TAG = "TeamListFragment"
    }
}

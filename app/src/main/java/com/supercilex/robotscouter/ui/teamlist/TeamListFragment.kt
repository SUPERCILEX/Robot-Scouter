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
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.client.spreadsheet.ExportService
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.data.IO_PERMS
import com.supercilex.robotscouter.util.data.TeamsLiveData
import com.supercilex.robotscouter.util.data.observeOnDataChanged
import com.supercilex.robotscouter.util.data.observeOnce
import com.supercilex.robotscouter.util.ui.FragmentBase
import com.supercilex.robotscouter.util.ui.OnBackPressedListener
import com.supercilex.robotscouter.util.ui.PermissionRequestHandler

class TeamListFragment : FragmentBase(), OnBackPressedListener, OnSuccessListener<Nothing?> {
    private val holder: TeamListHolder by lazy {
        ViewModelProviders.of(this).get(TeamListHolder::class.java)
                .also { onHolderReadyTask.setResult(it) }
    }
    private val onHolderReadyTask = TaskCompletionSource<TeamListHolder>()

    private val rootView: View by lazy { View.inflate(context, R.layout.fragment_team_list, null) }
    private val recyclerView: RecyclerView by lazy { rootView.findViewById<RecyclerView>(R.id.list) }
    private val menuHelper: TeamMenuHelper by lazy { TeamMenuHelper(this, recyclerView) }
    val permHandler: PermissionRequestHandler by lazy {
        PermissionRequestHandler(IO_PERMS, this, this)
    }

    private var adapter: TeamListAdapter? = null
    private val fab: FloatingActionButton by lazy { activity.findViewById<FloatingActionButton>(R.id.fab) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        holder.init(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater?,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.setHasFixedSize(true)
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                if (dy > 0) {
                    // User scrolled down -> hide the FAB
                    fab.hide()
                } else if (dy < 0 && !menuHelper.areTeamsSelected()) {
                    fab.show()
                }
            }
        })

        TeamsLiveData.observe(this, Observer { snapshots ->
            adapter?.let {
                it.stopListening()
                lifecycle.removeObserver(it)
            }

            if (snapshots == null) {
                rootView.findViewById<View>(R.id.no_content_hint).visibility = View.VISIBLE
                selectTeam(null)
            } else {
                adapter = TeamListAdapter(
                        snapshots, this, menuHelper, holder.selectedTeamIdListener)
                recyclerView.adapter = adapter
                menuHelper.setAdapter(adapter)
                menuHelper.restoreState(savedInstanceState); savedInstanceState?.clear()
            }
        })

        return rootView
    }

    override fun onSaveInstanceState(outState: Bundle) {
        holder.onSaveInstanceState(outState)
        menuHelper.saveState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) =
            menuHelper.onCreateOptionsMenu(menu, inflater)

    override fun onOptionsItemSelected(item: MenuItem) = menuHelper.onOptionsItemSelected(item)

    fun selectTeam(team: Team?) = onHolderReadyTask.task.addOnSuccessListener { it.selectTeam(team) }

    override fun onBackPressed(): Boolean = menuHelper.onBackPressed()

    fun resetMenu() = menuHelper.resetMenu()

    fun exportAllTeams() = onSuccess(null)

    override fun onSuccess(nothing: Nothing?) {
        if (menuHelper.areTeamsSelected()) {
            menuHelper.exportTeams()
        } else {
            TeamsLiveData.observeOnDataChanged().observeOnce {
                ExportService.exportAndShareSpreadSheet(this, permHandler, it)
                Tasks.forResult(null)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) =
            permHandler.onRequestPermissionsResult(requestCode, permissions, grantResults)

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) =
            permHandler.onActivityResult(requestCode)

    companion object {
        const val TAG = "TeamListFragment"
    }
}

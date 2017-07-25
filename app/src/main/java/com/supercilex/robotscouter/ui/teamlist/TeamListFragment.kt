package com.supercilex.robotscouter.ui.teamlist

import android.arch.lifecycle.LifecycleFragment
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

import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.data.model.Team

import com.supercilex.robotscouter.util.teamsListener

class TeamListFragment : LifecycleFragment(), OnBackPressedListener {
    private val holder: TeamListHolder by lazy {
        ViewModelProviders.of(this).get(TeamListHolder::class.java)
    }

    private val rootView: View by lazy { View.inflate(context, R.layout.fragment_team_list, null) }
    private val recyclerView: RecyclerView by lazy { rootView.findViewById<RecyclerView>(R.id.list) }
    private val menuHelper: TeamMenuHelper by lazy { TeamMenuHelper(this, recyclerView) }

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
                } else if (dy < 0 && menuHelper.noItemsSelected()) {
                    fab.show()
                }
            }
        })

        teamsListener.observe(this, Observer { snapshots ->
            cleanup()
            if (snapshots != null) {
                adapter = TeamListAdapter(
                        snapshots, this, menuHelper, holder.selectedTeamKeyListener)
                menuHelper.setAdapter(adapter)
                recyclerView.adapter = adapter
                menuHelper.restoreState(savedInstanceState)
            }
        })

        return rootView
    }

    override fun onSaveInstanceState(outState: Bundle) {
        holder.onSaveInstanceState(outState)
        menuHelper.saveState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanup()
        RobotScouter.getRefWatcher(activity).watch(this)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) =
            menuHelper.onCreateOptionsMenu(menu, inflater)

    override fun onOptionsItemSelected(item: MenuItem): Boolean = menuHelper.onOptionsItemSelected(item)

    fun selectTeam(team: Team?) {
        if (activity != null) holder.selectTeam(team)
    }

    private fun cleanup() {
        adapter?.cleanup()
        adapter?.notifyDataSetChanged()
        recyclerView.adapter = null
    }

    override fun onBackPressed(): Boolean = menuHelper.onBackPressed()

    fun resetMenu() = menuHelper.resetMenu()

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) =
            menuHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) =
            menuHelper.onActivityResult(requestCode)

    companion object {
        const val TAG = "TeamListFragment"
    }
}

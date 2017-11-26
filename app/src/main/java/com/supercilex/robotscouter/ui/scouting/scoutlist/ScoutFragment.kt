package com.supercilex.robotscouter.ui.scouting.scoutlist

import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.ui.scouting.MetricListFragment
import com.supercilex.robotscouter.util.data.getTabId
import com.supercilex.robotscouter.util.data.getTabIdBundle
import com.supercilex.robotscouter.util.data.getTeam
import com.supercilex.robotscouter.util.data.model.getScoutMetricsRef
import com.supercilex.robotscouter.util.data.model.trashScout
import com.supercilex.robotscouter.util.data.model.untrashScout
import com.supercilex.robotscouter.util.data.toBundle
import com.supercilex.robotscouter.util.ui.RecyclerPoolHolder
import com.supercilex.robotscouter.util.unsafeLazy
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.support.v4.find

class ScoutFragment : MetricListFragment() {
    private val team by unsafeLazy { arguments!!.getTeam() }
    private val scoutId by unsafeLazy { getTabId(arguments)!! }
    override val metricsRef by unsafeLazy { team.getScoutMetricsRef(scoutId) }

    val toolbar: Toolbar by unsafeLazy {
        parentFragment!!.find<Toolbar>(R.id.toolbar)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView.recycledViewPool = (parentFragment as RecyclerPoolHolder).recyclerPool
    }

    override fun onCreateRecyclerAdapter(savedInstanceState: Bundle?) = ScoutAdapter(
            holder.metrics,
            this,
            childFragmentManager,
            recyclerView,
            savedInstanceState
    )

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) =
            toolbar.inflateMenu(R.menu.scout_options)

    override fun onOptionsItemSelected(item: MenuItem) = if (item.itemId == R.id.action_delete) {
        team.trashScout(scoutId)
        longSnackbar(toolbar, R.string.scout_delete_message, R.string.undo) {
            team.untrashScout(scoutId)
            (parentFragment as ScoutListFragmentBase).pagerAdapter!!.currentTabId = scoutId
        }
        true
    } else {
        super.onOptionsItemSelected(item)
    }

    companion object {
        fun newInstance(scoutId: String, team: Team) = ScoutFragment().apply {
            arguments = getTabIdBundle(scoutId).apply { putAll(team.toBundle()) }
        }
    }
}

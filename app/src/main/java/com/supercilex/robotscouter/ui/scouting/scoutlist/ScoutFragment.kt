package com.supercilex.robotscouter.ui.scouting.scoutlist

import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import com.google.firebase.database.DatabaseReference
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.ui.scouting.MetricListAdapterBase
import com.supercilex.robotscouter.ui.scouting.MetricListFragment
import com.supercilex.robotscouter.util.data.getTabKey
import com.supercilex.robotscouter.util.data.getTabKeyBundle
import com.supercilex.robotscouter.util.data.model.deleteScout
import com.supercilex.robotscouter.util.data.model.getScoutMetricsRef
import com.supercilex.robotscouter.util.data.model.parseTeam
import com.supercilex.robotscouter.util.data.model.toBundle

class ScoutFragment : MetricListFragment() {
    override val metricsRef: DatabaseReference by lazy { getScoutMetricsRef(getTabKey(arguments)!!) }
    override val adapter: MetricListAdapterBase by lazy {
        ScoutAdapter(holder.metrics, childFragmentManager, recyclerView, this)
    }

    val toolbar: Toolbar by lazy { parentFragment.view!!.findViewById<Toolbar>(R.id.toolbar) }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) =
            toolbar.inflateMenu(R.menu.scout_options)

    override fun onOptionsItemSelected(item: MenuItem) = if (item.itemId == R.id.action_delete) {
        parseTeam(arguments).deleteScout(metricsRef.parent.key)
        metricsRef.parent.removeValue()
        true
    } else super.onOptionsItemSelected(item)

    companion object {
        fun newInstance(scoutKey: String, team: Team) = ScoutFragment().apply {
            arguments = getTabKeyBundle(scoutKey).apply { putAll(team.toBundle()) }
        }
    }
}

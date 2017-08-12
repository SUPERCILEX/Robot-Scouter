package com.supercilex.robotscouter.ui.scouting.scoutlist

import com.google.firebase.database.DatabaseReference
import com.supercilex.robotscouter.ui.scouting.MetricListAdapterBase
import com.supercilex.robotscouter.ui.scouting.MetricListFragment
import com.supercilex.robotscouter.util.data.getTabKey
import com.supercilex.robotscouter.util.data.getTabKeyBundle
import com.supercilex.robotscouter.util.data.model.getScoutMetricsRef

class ScoutFragment : MetricListFragment() {
    override val metricsRef: DatabaseReference by lazy { getScoutMetricsRef(getTabKey(arguments)!!) }
    override val adapter: MetricListAdapterBase by lazy {
        ScoutAdapter(holder.metrics, childFragmentManager, recyclerView, this)
    }

    companion object {
        fun newInstance(scoutKey: String): ScoutFragment =
                ScoutFragment().apply { arguments = getTabKeyBundle(scoutKey) }
    }
}

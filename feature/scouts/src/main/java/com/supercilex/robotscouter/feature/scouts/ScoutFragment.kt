package com.supercilex.robotscouter.feature.scouts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.supercilex.robotscouter.core.data.asLiveData
import com.supercilex.robotscouter.core.data.getTabId
import com.supercilex.robotscouter.core.data.getTabIdBundle
import com.supercilex.robotscouter.core.data.getTeam
import com.supercilex.robotscouter.core.data.model.getScoutMetricsRef
import com.supercilex.robotscouter.core.data.model.trashScout
import com.supercilex.robotscouter.core.data.model.untrashScout
import com.supercilex.robotscouter.core.data.toBundle
import com.supercilex.robotscouter.core.model.Team
import com.supercilex.robotscouter.core.ui.RecyclerPoolHolder
import com.supercilex.robotscouter.core.ui.observeNonNull
import com.supercilex.robotscouter.core.unsafeLazy
import com.supercilex.robotscouter.shared.scouting.MetricListFragment
import kotlinx.android.synthetic.main.fragment_scout_list_toolbar.*
import kotlinx.android.synthetic.main.fragment_scout_metric_list.*
import org.jetbrains.anko.design.longSnackbar
import com.supercilex.robotscouter.R as RC

internal class ScoutFragment : MetricListFragment() {
    private val team by unsafeLazy { checkNotNull(arguments).getTeam() }
    private val scoutId by unsafeLazy { checkNotNull(getTabId(arguments)) }
    override val metricsRef by unsafeLazy { team.getScoutMetricsRef(scoutId) }
    override val dataId get() = scoutId

    private val toolbar by unsafeLazy { requireActivity().scoutsToolbar }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_scout_metric_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        metricsView.setRecycledViewPool((parentFragment as RecyclerPoolHolder).recyclerPool)

        emptyScoutHint.show()
        holder.metrics.asLiveData().observeNonNull(viewLifecycleOwner) {
            if (it.isNotEmpty()) emptyScoutHint.hide()
        }
    }

    override fun onCreateRecyclerAdapter(savedInstanceState: Bundle?) = ScoutAdapter(
            this,
            holder.metrics,
            metricsView,
            savedInstanceState
    )

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) =
            toolbar.inflateMenu(R.menu.scout_options)

    override fun onOptionsItemSelected(item: MenuItem) = if (item.itemId == R.id.action_delete) {
        team.trashScout(scoutId)
        longSnackbar(toolbar, R.string.scout_delete_message, RC.string.undo) {
            team.untrashScout(scoutId)
            checkNotNull((parentFragment as ScoutListFragmentBase).pagerAdapter)
                    .currentTabId = scoutId
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

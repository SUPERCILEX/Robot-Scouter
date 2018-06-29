package com.supercilex.robotscouter.feature.scouts

import android.os.Bundle
import android.support.v7.widget.Toolbar
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
import com.supercilex.robotscouter.core.data.observeNonNull
import com.supercilex.robotscouter.core.data.toBundle
import com.supercilex.robotscouter.core.model.Team
import com.supercilex.robotscouter.core.ui.RecyclerPoolHolder
import com.supercilex.robotscouter.core.ui.views.ContentLoadingHint
import com.supercilex.robotscouter.core.unsafeLazy
import com.supercilex.robotscouter.shared.scouting.MetricListFragment
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.support.v4.find
import com.supercilex.robotscouter.R as RC

internal class ScoutFragment : MetricListFragment() {
    private val team by unsafeLazy { checkNotNull(arguments).getTeam() }
    private val scoutId by unsafeLazy { checkNotNull(getTabId(arguments)) }
    override val metricsRef by unsafeLazy { team.getScoutMetricsRef(scoutId) }
    override val dataId get() = scoutId

    private val toolbar by unsafeLazy { checkNotNull(parentFragment).find<Toolbar>(RC.id.toolbar) }
    private val emptyScoutHint by unsafeLazy { find<ContentLoadingHint>(RC.id.emptyScoutHint) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        holder.metrics.asLiveData().observeNonNull(this) {
            if (it.isNotEmpty()) emptyScoutHint.hide()
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View = View.inflate(context, RC.layout.fragment_scout_metric_list, null)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        metricsView.recycledViewPool = (parentFragment as RecyclerPoolHolder).recyclerPool
        emptyScoutHint.show()
    }

    override fun onCreateRecyclerAdapter(savedInstanceState: Bundle?) = ScoutAdapter(
            holder.metrics,
            this,
            childFragmentManager,
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

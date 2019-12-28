package com.supercilex.robotscouter.feature.scouts

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Guideline
import androidx.core.view.ViewCompat
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
import com.supercilex.robotscouter.core.ui.isInTabletMode
import com.supercilex.robotscouter.core.ui.longSnackbar
import com.supercilex.robotscouter.core.unsafeLazy
import com.supercilex.robotscouter.shared.scouting.MetricListFragment
import kotlinx.android.synthetic.main.fragment_scout_list_toolbar.*
import kotlinx.android.synthetic.main.fragment_scout_metric_list.*
import com.supercilex.robotscouter.R as RC

internal class ScoutFragment : MetricListFragment(R.layout.fragment_scout_metric_list),
        View.OnLayoutChangeListener {
    private val team by unsafeLazy { requireArguments().getTeam() }
    private val scoutId by unsafeLazy { checkNotNull(getTabId(arguments)) }
    override val metricsRef by unsafeLazy { team.getScoutMetricsRef(scoutId) }
    override val dataId get() = scoutId

    private val toolbar by unsafeLazy { requireActivity().scoutsToolbar }

    private val homeDivider by unsafeLazy {
        if (requireContext().isInTabletMode()) {
            requireActivity().findViewById<Guideline>(RC.id.guideline)
        } else {
            null
        }
    }
    private val minimizedPadding by unsafeLazy {
        resources.getDimensionPixelSize(RC.dimen.list_item_padding_horizontal) to
                resources.getDimensionPixelSize(RC.dimen.list_item_padding_vertical)
    }
    private val expandedPadding by unsafeLazy {
        resources.getDimensionPixelSize(RC.dimen.list_item_padding_horizontal_scout) to
                resources.getDimensionPixelSize(RC.dimen.list_item_padding_vertical_scout)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        metricsView.setRecycledViewPool((parentFragment as RecyclerPoolHolder).recyclerPool)
        metricsView.addOnLayoutChangeListener(this)

        emptyScoutHint.show()
        holder.metrics.asLiveData().observe(viewLifecycleOwner) {
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

    override fun onDestroyView() {
        super.onDestroyView()
        metricsView.removeOnLayoutChangeListener(this)
    }

    override fun onOptionsItemSelected(item: MenuItem) = if (item.itemId == R.id.action_delete) {
        team.trashScout(scoutId)
        toolbar.longSnackbar(R.string.scout_delete_message, RC.string.undo) {
            team.untrashScout(scoutId)
            checkNotNull((parentFragment as ScoutListFragmentBase).pagerAdapter)
                    .currentTabId = scoutId
        }
        true
    } else {
        super.onOptionsItemSelected(item)
    }

    override fun onLayoutChange(
            v: View,
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
            oldLeft: Int,
            oldTop: Int,
            oldRight: Int,
            oldBottom: Int
    ) {
        val div = homeDivider ?: return
        val guide = (div.layoutParams as ConstraintLayout.LayoutParams).guidePercent

        val metricsView = metricsView
        val (horizontal, vertical) = if (guide > .5) minimizedPadding else expandedPadding
        metricsView.post {
            ViewCompat.setPaddingRelative(metricsView, horizontal, vertical, horizontal, vertical)
        }
    }

    companion object {
        fun newInstance(scoutId: String, team: Team) = ScoutFragment().apply {
            arguments = getTabIdBundle(scoutId).apply { putAll(team.toBundle()) }
        }
    }
}

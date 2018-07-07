package com.supercilex.robotscouter.feature.scouts

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.Window
import androidx.fragment.app.Fragment
import androidx.fragment.app.transaction
import com.supercilex.robotscouter.Bridge
import com.supercilex.robotscouter.TabletScoutListFragmentBridge
import com.supercilex.robotscouter.TabletScoutListFragmentCompanion
import com.supercilex.robotscouter.core.ValueSeeker
import com.supercilex.robotscouter.core.data.toBundle
import com.supercilex.robotscouter.core.model.Team
import com.supercilex.robotscouter.core.ui.TeamSelectionListener
import com.supercilex.robotscouter.core.ui.animatePopReveal
import com.supercilex.robotscouter.core.ui.isInTabletMode
import org.jetbrains.anko.findOptional
import com.supercilex.robotscouter.R as RC

@Bridge
internal class TabletScoutListFragment : ScoutListFragmentBase(), TabletScoutListFragmentBridge {
    private val noContentHint by ValueSeeker {
        requireActivity().findOptional<View>(RC.id.noTeamSelectedHint)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!requireContext().isInTabletMode()) {
            val listener = context as TeamSelectionListener
            listener.onTeamSelected(bundle, true)
            removeFragment()
        }
    }

    override fun newViewModel(savedInstanceState: Bundle?) = object : AppBarViewHolderBase(
            this, savedInstanceState, dataHolder.teamListener, onScoutingReadyTask.task) {
        init {
            toolbar.setOnMenuItemClickListener {
                // We need to be able to guarantee that our `onOptionsItemSelected`s are called
                // before that of TeamListActivity because of duplicate menu item ids
                Fragment::class.java
                        .getDeclaredMethod("performOptionsItemSelected", MenuItem::class.java)
                        .apply { isAccessible = true }
                        .invoke(this@TabletScoutListFragment, it) as Boolean ||
                        requireActivity().onMenuItemSelected(Window.FEATURE_OPTIONS_PANEL, it)
            }
        }
    }

    override fun onChanged(team: Team?) {
        super.onChanged(team)
        select(team)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        noContentHint?.animatePopReveal(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        select(null)
        noContentHint?.animatePopReveal(true)
    }

    override fun onTeamDeleted() = removeFragment()

    private fun select(team: Team?) {
        requireFragmentManager().fragments.filterIsInstance<TeamSelectionListener>().forEach {
            it.onTeamSelected(team?.toBundle() ?: Bundle.EMPTY)
        }
    }

    private fun removeFragment() {
        requireFragmentManager().transaction { remove(this@TabletScoutListFragment) }
    }

    companion object : TabletScoutListFragmentCompanion {
        override fun newInstance(args: Bundle) =
                TabletScoutListFragment().apply { arguments = args }
    }
}

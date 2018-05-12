package com.supercilex.robotscouter.feature.scouts

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.MenuItem
import android.view.View
import android.view.Window
import com.supercilex.robotscouter.core.data.toBundle
import com.supercilex.robotscouter.core.model.Team
import com.supercilex.robotscouter.core.ui.TeamSelectionListener
import com.supercilex.robotscouter.core.ui.animatePopReveal
import com.supercilex.robotscouter.core.ui.isInTabletMode
import kotlinx.android.synthetic.main.fragment_scout_list.*
import org.jetbrains.anko.findOptional

class TabletScoutListFragment : ScoutListFragmentBase() {
    private var noContentHint: View? = null
        get() {
            if (field == null) field = requireActivity().findOptional(R.id.noTeamSelectedHint)
            return field
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
        requireFragmentManager().beginTransaction().remove(this).commit()
    }

    companion object {
        const val TAG = "TabletScoutListFrag"

        fun newInstance(args: Bundle): ScoutListFragmentBase =
                TabletScoutListFragment().apply { arguments = args }
    }
}

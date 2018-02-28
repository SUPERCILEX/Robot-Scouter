package com.supercilex.robotscouter.ui.teamlist

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.MenuItem
import android.view.View
import android.view.Window
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.ui.scouting.scoutlist.AppBarViewHolderBase
import com.supercilex.robotscouter.ui.scouting.scoutlist.ScoutListFragmentBase
import com.supercilex.robotscouter.util.ui.TeamSelectionListener
import com.supercilex.robotscouter.util.ui.animatePopReveal
import com.supercilex.robotscouter.util.ui.isInTabletMode
import org.jetbrains.anko.findOptional

class TabletScoutListFragment : ScoutListFragmentBase() {
    private var noContentHint: View? = null
        get() {
            if (field == null) field = requireActivity().findOptional(R.id.no_team_selected_hint)
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
            this, savedInstanceState, view!!, dataHolder.teamListener, onScoutingReadyTask.task) {
        init {
            toolbar.setOnMenuItemClickListener {
                // We need to be able to guarantee that our `onOptionsItemSelected`s are called
                // before that of TeamListActivity because of duplicate menu item ids
                Fragment::class.java
                        .getDeclaredMethod("performOptionsItemSelected", MenuItem::class.java)
                        .apply { isAccessible = true }
                        .invoke(this@TabletScoutListFragment, it) as Boolean
                        || requireActivity().onMenuItemSelected(Window.FEATURE_OPTIONS_PANEL, it)
            }
        }
    }

    override fun onChanged(team: Team?) {
        super.onChanged(team)
        (activity as TeamListActivity).teamListFragment.selectTeam(team ?: return)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        noContentHint?.animatePopReveal(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        (activity as TeamListActivity).teamListFragment.selectTeam(null)
        noContentHint?.animatePopReveal(true)
    }

    override fun onTeamDeleted() = removeFragment()

    private fun removeFragment() {
        requireFragmentManager().beginTransaction().remove(this).commit()
    }

    companion object {
        const val TAG = "TabletScoutListFrag"

        fun newInstance(args: Bundle): ScoutListFragmentBase =
                TabletScoutListFragment().apply { arguments = args }
    }
}

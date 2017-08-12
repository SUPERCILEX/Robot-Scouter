package com.supercilex.robotscouter.ui.teamlist

import android.os.Bundle
import android.view.View
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.ui.scouting.scoutlist.AppBarViewHolderBase
import com.supercilex.robotscouter.ui.scouting.scoutlist.ScoutListFragmentBase
import com.supercilex.robotscouter.util.ui.isInTabletMode

class TabletScoutListFragment : ScoutListFragmentBase() {
    override val viewHolder: AppBarViewHolderBase by lazy {
        object : AppBarViewHolderBase(
                this, rootView, dataHolder.teamListener, onScoutingReadyTask.task) {
            init {
                toolbar.inflateMenu(R.menu.scout)
                toolbar.setOnMenuItemClickListener { onOptionsItemSelected(it) }
                initMenu(toolbar.menu)
            }
        }
    }
    private var hint: View? = null
        get() {
            if (field == null) field = activity.findViewById(R.id.no_team_selected_hint)
            return field
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isInTabletMode(context)) {
            val listener = context as TeamSelectionListener
            listener.onTeamSelected(bundle, true)
            removeFragment()
        }
    }

    override fun onChanged(team: Team?) {
        super.onChanged(team)
        (activity as TeamListActivity).teamListFragment.selectTeam(team ?: return)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        hint?.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        (activity as TeamListActivity).teamListFragment.selectTeam(null)
        hint?.visibility = View.VISIBLE
    }

    override fun onTeamDeleted() = removeFragment()

    private fun removeFragment() {
        fragmentManager.beginTransaction().remove(this).commit()
    }

    companion object {
        fun newInstance(args: Bundle): ScoutListFragmentBase =
                TabletScoutListFragment().apply { arguments = args }
    }
}

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
import com.supercilex.robotscouter.util.ui.isInTabletMode

class TabletScoutListFragment : ScoutListFragmentBase() {
    override val viewHolder: AppBarViewHolderBase by lazy {
        object : AppBarViewHolderBase(
                this, rootView, dataHolder.teamListener, onScoutingReadyTask.task) {
            init {
                toolbar.setOnMenuItemClickListener {
                    // We need to be able to guarantee that our `onOptionsItemSelected`s are called
                    // before that of TeamListActivity because of duplicate menu item ids
                    Fragment::class.java
                            .getDeclaredMethod("performOptionsItemSelected", MenuItem::class.java)
                            .apply { isAccessible = true }
                            .invoke(this@TabletScoutListFragment, it) as Boolean
                            || activity.onMenuItemSelected(Window.FEATURE_OPTIONS_PANEL, it)
                }
            }
        }
    }
    private var hint: View? = null; get() {
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

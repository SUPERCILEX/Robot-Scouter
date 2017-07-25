package com.supercilex.robotscouter.ui.teamlist

import android.arch.lifecycle.LiveData
import android.os.Bundle
import android.view.View
import com.google.android.gms.tasks.Task
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.util.TeamHelper
import com.supercilex.robotscouter.ui.scout.AppBarViewHolderBase
import com.supercilex.robotscouter.ui.scout.ScoutListFragmentBase
import com.supercilex.robotscouter.util.isInTabletMode

class TabletScoutListFragment : ScoutListFragmentBase() {
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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        hint?.visibility = View.GONE
    }

    override fun newAppBarViewHolder(listener: LiveData<TeamHelper>,
                                     onScoutingReadyTask: Task<Void>): AppBarViewHolderBase =
            object : AppBarViewHolderBase(listener, this, mRootView, onScoutingReadyTask) {
                init {
                    mToolbar.inflateMenu(R.menu.scout)
                    mToolbar.setOnMenuItemClickListener({
                        this@TabletScoutListFragment.onOptionsItemSelected(it)
                    })
                    initMenu(mToolbar.menu)
                }
            }

    override fun onDestroy() {
        super.onDestroy()
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

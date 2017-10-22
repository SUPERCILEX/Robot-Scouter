package com.supercilex.robotscouter.ui.scouting.scoutlist

import android.support.design.widget.Snackbar
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import com.google.firebase.firestore.Query
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.ui.scouting.TabPagerAdapterBase
import com.supercilex.robotscouter.util.FIRESTORE_TIMESTAMP
import com.supercilex.robotscouter.util.data.model.delete
import com.supercilex.robotscouter.util.data.model.getScoutRef
import com.supercilex.robotscouter.util.isOffline

class ScoutPagerAdapter(fragment: Fragment, tabLayout: TabLayout, private val team: Team) :
        TabPagerAdapterBase(fragment, tabLayout, team.getScoutRef()) {
    override val editTabNameRes = R.string.scout_edit_name_title

    init {
        holder.init(dataRef.orderBy(FIRESTORE_TIMESTAMP, Query.Direction.DESCENDING))
    }

    override fun getItem(position: Int) =
            ScoutFragment.newInstance(holder.scouts[position].id, team)

    override fun getPageTitle(position: Int): String =
            fragment.getString(R.string.scout_tab_default_title, count - position)

    override fun onDataChanged() {
        if (oldScouts.isNotEmpty() && holder.scouts.isEmpty() && !isOffline() && fragment.isResumed) {
            Snackbar.make(fragment.view!!,
                          fragment.getString(R.string.scout_should_delete_team_message, team),
                          Snackbar.LENGTH_LONG)
                    .setAction(R.string.scout_delete_team_title) { team.delete() }
                    .show()
        }
        super.onDataChanged()
    }
}

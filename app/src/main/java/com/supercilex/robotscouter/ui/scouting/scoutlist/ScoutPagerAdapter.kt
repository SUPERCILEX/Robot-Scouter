package com.supercilex.robotscouter.ui.scouting.scoutlist

import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import com.google.firebase.firestore.Query
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.ui.scouting.TabPagerAdapterBase
import com.supercilex.robotscouter.util.data.QueryGenerator
import com.supercilex.robotscouter.util.data.model.getScoutsQuery
import com.supercilex.robotscouter.util.data.model.getScoutsRef
import com.supercilex.robotscouter.util.logSelectScout

class ScoutPagerAdapter(
        fragment: Fragment,
        tabLayout: TabLayout,
        private val team: Team
) : TabPagerAdapterBase(fragment, tabLayout, team.getScoutsRef()) {
    override val editTabNameRes = R.string.scout_edit_name_title

    init {
        holder.init(generator(team))
    }

    override fun getItem(position: Int) =
            ScoutFragment.newInstance(currentScouts[position].id, team)

    override fun getPageTitle(position: Int): String =
            fragment.getString(R.string.scout_tab_default_title, count - position)

    override fun onTabSelected(tab: TabLayout.Tab) {
        super.onTabSelected(tab)
        currentTabId?.let { tabId ->
            team.logSelectScout(tabId, currentScouts.find { it.id == tabId }!!.templateId)
        }
    }

    private companion object {
        // Purposeful indirection to avoid memory leaks
        val generator: (Team) -> QueryGenerator = { team ->
            { team.getScoutsQuery(Query.Direction.DESCENDING) }
        }
    }
}

package com.supercilex.robotscouter.feature.scouts

import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import com.google.firebase.firestore.Query
import com.supercilex.robotscouter.core.data.QueryGenerator
import com.supercilex.robotscouter.core.data.logSelectScout
import com.supercilex.robotscouter.core.data.model.getScoutsQuery
import com.supercilex.robotscouter.core.data.model.getScoutsRef
import com.supercilex.robotscouter.core.model.Team
import com.supercilex.robotscouter.shared.scouting.TabPagerAdapterBase

class ScoutPagerAdapter(
        fragment: Fragment,
        private val team: Team
) : TabPagerAdapterBase(fragment, team.getScoutsRef()) {
    override val editTabNameRes = R.string.scout_edit_name_title

    init {
        holder.init(generator(team))
        init()
    }

    override fun getItem(position: Int): Fragment =
            ScoutFragment.newInstance(currentScouts[position].id, team)

    override fun getPageTitle(position: Int): String =
            fragment.getString(R.string.scout_default_name, count - position)

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

package com.supercilex.robotscouter.feature.scouts

import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.Query
import com.supercilex.robotscouter.core.data.QueryGenerator
import com.supercilex.robotscouter.core.data.logSelectScout
import com.supercilex.robotscouter.core.data.model.getScoutsQuery
import com.supercilex.robotscouter.core.data.model.scoutsRef
import com.supercilex.robotscouter.core.model.Team
import com.supercilex.robotscouter.core.ui.LifecycleAwareLazy
import com.supercilex.robotscouter.core.ui.onDestroy
import com.supercilex.robotscouter.shared.scouting.TabPagerAdapterBase
import org.jetbrains.anko.find
import com.supercilex.robotscouter.R as RC

internal class ScoutPagerAdapter(
        fragment: Fragment,
        private val team: Team
) : TabPagerAdapterBase(fragment, team.scoutsRef) {
    override val editTabNameRes = R.string.scout_edit_name_title
    override val tabs by fragment.LifecycleAwareLazy {
        fragment.requireActivity().find<TabLayout>(R.id.tabs)
    } onDestroy {
        it.setupWithViewPager(null)
    }

    init {
        holder.init(generator(team))
        init()
    }

    override fun getItem(position: Int): Fragment =
            ScoutFragment.newInstance(currentScouts[position].id, team)

    override fun getPageTitle(position: Int): String =
            fragment.getString(RC.string.scout_default_name, count - position)

    override fun onTabSelected(tab: TabLayout.Tab) {
        super.onTabSelected(tab)
        val tabId = checkNotNull(currentTabId)
        team.logSelectScout(tabId, currentScouts.first { it.id == tabId }.templateId)
    }

    private companion object {
        // Purposeful indirection to avoid memory leaks
        val generator: (Team) -> QueryGenerator = { team ->
            { team.getScoutsQuery(Query.Direction.DESCENDING) }
        }
    }
}

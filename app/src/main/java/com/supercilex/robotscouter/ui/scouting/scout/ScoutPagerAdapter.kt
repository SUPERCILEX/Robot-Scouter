package com.supercilex.robotscouter.ui.scouting.scout

import android.arch.lifecycle.LifecycleFragment
import android.support.design.widget.TabLayout
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.ui.scouting.TabPagerAdapterBase
import com.supercilex.robotscouter.util.FIREBASE_SCOUTS
import com.supercilex.robotscouter.util.SINGLE_ITEM
import com.supercilex.robotscouter.util.data.model.getScoutIndicesRef
import com.supercilex.robotscouter.util.isOffline

class ScoutPagerAdapter(fragment: LifecycleFragment,
                        tabLayout: TabLayout,
                        private val appBarViewHolder: AppBarViewHolderBase,
                        private val team: Team) :
        TabPagerAdapterBase(fragment, tabLayout, getScoutIndicesRef(team.key), FIREBASE_SCOUTS) {
    init {
        init()
    }

    override fun getItem(position: Int) = ScoutFragment.newInstance(keys[position])

    override fun getPageTitle(position: Int): String =
            fragment.getString(R.string.title_scout_tab, count - position)

    override fun onChanged(newKeys: List<String>?) {
        val hadTabs = !keys.isEmpty()
        super.onChanged(newKeys)
        if (hadTabs && keys.isEmpty() && !isOffline() && fragment.isResumed) {
            ShouldDeleteTeamDialog.show(fragment.childFragmentManager, team)
        }
        appBarViewHolder.setDeleteScoutMenuItemVisible(!keys.isEmpty())
    }

    fun deleteScout() {
        val index = keys.indexOf(currentTabKey)
        var newKey: String? = null
        if (keys.size > SINGLE_ITEM) {
            newKey = if (keys.size - 1 > index) keys[index + 1] else keys[index - 1]
        }
        com.supercilex.robotscouter.util.data.model.deleteScout(holder.ref.key, currentTabKey!!)
        currentTabKey = newKey
    }
}

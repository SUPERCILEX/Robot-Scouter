package com.supercilex.robotscouter.feature.templates

import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.Query
import com.supercilex.robotscouter.core.data.model.getTemplatesQuery
import com.supercilex.robotscouter.core.data.templatesRef
import com.supercilex.robotscouter.core.ui.LifecycleAwareLazy
import com.supercilex.robotscouter.core.ui.onDestroy
import com.supercilex.robotscouter.shared.scouting.TabPagerAdapterBase
import com.supercilex.robotscouter.R as RC

internal open class TemplatePagerAdapter(fragment: Fragment) :
        TabPagerAdapterBase(fragment, templatesRef) {
    override val editTabNameRes = R.string.template_edit_name_title
    override val tabs: TabLayout by fragment.LifecycleAwareLazy {
        fragment.requireActivity().findViewById<TabLayout>(R.id.tabs)
    } onDestroy {
        it.setupWithViewPager(null)
    }

    init {
        holder.init { getTemplatesQuery(Query.Direction.DESCENDING) }
        init()
    }

    override fun getItem(position: Int) = TemplateFragment.newInstance(currentScouts[position].id)

    override fun getPageTitle(position: Int): String =
            fragment.getString(RC.string.template_tab_default_title, count - position)
}

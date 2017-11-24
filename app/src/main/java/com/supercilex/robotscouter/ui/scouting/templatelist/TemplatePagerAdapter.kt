package com.supercilex.robotscouter.ui.scouting.templatelist

import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import com.google.firebase.appindexing.FirebaseUserActions
import com.google.firebase.appindexing.builders.Actions
import com.google.firebase.firestore.Query
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.ui.scouting.TabPagerAdapterBase
import com.supercilex.robotscouter.util.data.getTemplateLink
import com.supercilex.robotscouter.util.data.model.getTemplatesQuery
import com.supercilex.robotscouter.util.logSelectTemplate
import com.supercilex.robotscouter.util.templates

open class TemplatePagerAdapter(
        fragment: Fragment,
        tabLayout: TabLayout
) : TabPagerAdapterBase(fragment, tabLayout, templates) {
    override val editTabNameRes = R.string.template_edit_name_title

    init {
        holder.init(getTemplatesQuery(Query.Direction.DESCENDING))
    }

    override fun getItem(position: Int) = TemplateFragment.newInstance(currentScouts[position].id)

    override fun getPageTitle(position: Int): String =
            fragment.getString(R.string.template_tab_default_title, count - position)

    override fun onTabSelected(tab: TabLayout.Tab) {
        super.onTabSelected(tab)
        currentTabId?.let {
            logSelectTemplate(it, tab.text.toString())
            FirebaseUserActions.getInstance().end(
                    Actions.newView(tab.text.toString(), getTemplateLink(it)))
        }
    }
}

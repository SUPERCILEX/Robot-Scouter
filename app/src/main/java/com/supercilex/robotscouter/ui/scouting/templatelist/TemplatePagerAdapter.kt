package com.supercilex.robotscouter.ui.scouting.templatelist

import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import com.google.firebase.firestore.Query
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.ui.scouting.TabPagerAdapterBase
import com.supercilex.robotscouter.util.FIRESTORE_TEMPLATES
import com.supercilex.robotscouter.util.data.model.getTemplatesQuery

open class TemplatePagerAdapter(fragment: Fragment, tabLayout: TabLayout) :
        TabPagerAdapterBase(fragment, tabLayout, FIRESTORE_TEMPLATES) {
    override val editTabNameRes = R.string.title_edit_template_name

    init {
        holder.init(getTemplatesQuery(Query.Direction.DESCENDING))
    }

    override fun getItem(position: Int) = TemplateFragment.newInstance(holder.scouts[position].id)

    override fun getPageTitle(position: Int): String =
            fragment.getString(R.string.title_template_tab, count - position)
}

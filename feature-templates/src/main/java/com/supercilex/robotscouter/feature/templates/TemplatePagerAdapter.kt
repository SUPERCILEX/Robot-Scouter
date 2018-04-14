package com.supercilex.robotscouter.feature.templates

import android.support.v4.app.Fragment
import com.google.firebase.firestore.Query
import com.supercilex.robotscouter.core.data.model.getTemplatesQuery
import com.supercilex.robotscouter.core.data.templatesRef
import com.supercilex.robotscouter.shared.scouting.TabPagerAdapterBase

internal open class TemplatePagerAdapter(fragment: Fragment) : TabPagerAdapterBase(fragment, templatesRef) {
    override val editTabNameRes = R.string.template_edit_name_title

    init {
        holder.init { getTemplatesQuery(Query.Direction.DESCENDING) }
        init()
    }

    override fun getItem(position: Int) = TemplateFragment.newInstance(currentScouts[position].id)

    override fun getPageTitle(position: Int): String =
            fragment.getString(R.string.template_tab_default_title, count - position)
}

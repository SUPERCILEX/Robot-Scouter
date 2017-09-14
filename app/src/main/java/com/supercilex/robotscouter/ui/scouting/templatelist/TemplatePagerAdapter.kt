package com.supercilex.robotscouter.ui.scouting.templatelist

import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.ui.scouting.TabPagerAdapterBase
import com.supercilex.robotscouter.util.FIREBASE_TEMPLATES
import com.supercilex.robotscouter.util.data.model.templateIndicesRef

open class TemplatePagerAdapter(fragment: Fragment, tabLayout: TabLayout) :
        TabPagerAdapterBase(fragment, tabLayout, templateIndicesRef, FIREBASE_TEMPLATES) {
    override val editTabNameRes = R.string.title_edit_template_name

    init {
        init()
    }

    override fun getItem(position: Int) = TemplateFragment.newInstance(keys[position])

    override fun getPageTitle(position: Int): String =
            fragment.getString(R.string.title_template_tab, count - position)
}

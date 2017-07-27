package com.supercilex.robotscouter.ui.scouting.template

import android.arch.lifecycle.LifecycleFragment
import android.support.design.widget.TabLayout
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.ui.scouting.TabPagerAdapterBase
import com.supercilex.robotscouter.util.FIREBASE_TEMPLATES
import com.supercilex.robotscouter.util.data.model.templateIndicesRef

class TemplatePagerAdapter(fragment: LifecycleFragment, tabLayout: TabLayout) :
        TabPagerAdapterBase(fragment, tabLayout, templateIndicesRef, FIREBASE_TEMPLATES) {
    init {
        init()
    }

    override fun getItem(position: Int) = TemplateFragment.newInstance(keys[position])

    override fun getPageTitle(position: Int): String =
            fragment.getString(R.string.title_template_tab, count - position)
}

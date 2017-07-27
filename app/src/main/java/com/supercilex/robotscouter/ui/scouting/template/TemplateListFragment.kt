package com.supercilex.robotscouter.ui.scouting.template

import android.arch.lifecycle.LifecycleFragment
import android.os.Bundle
import android.support.annotation.DrawableRes
import android.support.annotation.IdRes
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.support.v7.content.res.AppCompatResources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.clans.fab.FloatingActionButton
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.ui.teamlist.OnBackPressedListener
import com.supercilex.robotscouter.util.data.getTabKey
import com.supercilex.robotscouter.util.data.getTabKeyBundle

class TemplateListFragment : LifecycleFragment(), View.OnClickListener, OnBackPressedListener {
    private val rootView by lazy { View.inflate(context, R.layout.fragment_template_list, null) }
    private val pagerAdapter by lazy {
        val tabLayout = rootView.findViewById<TabLayout>(R.id.tabs)
        val viewPager = rootView.findViewById<ViewPager>(R.id.viewpager)
        val adapter = TemplatePagerAdapter(this, tabLayout)

        viewPager.adapter = adapter
        tabLayout.setupWithViewPager(viewPager)

        adapter
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        fun initFab(@IdRes id: Int, @DrawableRes icon: Int) {
            val fab: FloatingActionButton = rootView.findViewById(id)
            fab.setOnClickListener(this)
            fab.setImageDrawable(AppCompatResources.getDrawable(fab.context, icon))
        }
        initFab(R.id.add_header, R.drawable.ic_title_white_24dp)
        initFab(R.id.add_checkbox, R.drawable.ic_done_white_24dp)
        initFab(R.id.add_stopwatch, R.drawable.ic_timer_white_24dp)
        initFab(R.id.add_note, R.drawable.ic_note_white_24dp)
        initFab(R.id.add_counter, R.drawable.ic_count_white_24dp)
        initFab(R.id.add_spinner, R.drawable.ic_list_white_24dp)

        pagerAdapter.currentTabKey = getTabKey(savedInstanceState ?: arguments)

        return rootView
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val activity = activity as AppCompatActivity
        activity.setSupportActionBar(rootView.findViewById(R.id.toolbar))
        activity.supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSaveInstanceState(outState: Bundle) = pagerAdapter.onSaveInstanceState(outState)

    override fun onClick(v: View) = childFragmentManager.fragments
            .filterIsInstance<TemplateFragment>()
            .filter { it.userVisibleHint }
            .forEach { it.onClick(v) }

    override fun onBackPressed(): Boolean =
            childFragmentManager.fragments.any { it is OnBackPressedListener && it.onBackPressed() }

    companion object {
        const val TAG = "TemplateListFragment"

        fun newInstance(templateKey: String?) =
                TemplateListFragment().apply { arguments = getTabKeyBundle(templateKey) }
    }
}


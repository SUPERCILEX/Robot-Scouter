package com.supercilex.robotscouter.ui.scouting.templatelist

import android.arch.lifecycle.LifecycleFragment
import android.os.Bundle
import android.support.annotation.DrawableRes
import android.support.annotation.IdRes
import android.support.design.widget.Snackbar
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.support.v7.content.res.AppCompatResources
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.github.clans.fab.FloatingActionButton
import com.github.clans.fab.FloatingActionMenu
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.ui.teamlist.OnBackPressedListener
import com.supercilex.robotscouter.util.SINGLE_ITEM
import com.supercilex.robotscouter.util.data.defaultTemplateKey
import com.supercilex.robotscouter.util.data.getTabKey
import com.supercilex.robotscouter.util.data.getTabKeyBundle

class TemplateListFragment : LifecycleFragment(), View.OnClickListener, OnBackPressedListener {
    private val rootView by lazy { View.inflate(context, R.layout.fragment_template_list, null) }
    val fam: FloatingActionMenu by lazy { rootView.findViewById<FloatingActionMenu>(R.id.fab_menu) }
    private val pagerAdapter by lazy {
        val tabLayout = rootView.findViewById<TabLayout>(R.id.tabs)
        val viewPager = rootView.findViewById<ViewPager>(R.id.viewpager)
        val adapter = object : TemplatePagerAdapter(this, tabLayout) {
            override fun onChanged(newKeys: List<String>?) {
                super.onChanged(newKeys)
                if (newKeys!!.isEmpty()) fam.hideMenuButton(true) else fam.showMenuButton(true)
            }

            override fun onTabSelected(tab: TabLayout.Tab) {
                super.onTabSelected(tab)
                fam.close(true)
                fam.showMenuButton(true)
            }
        }

        viewPager.adapter = adapter
        tabLayout.setupWithViewPager(viewPager)

        adapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) =
            inflater.inflate(R.menu.template_list_menu, menu)

    override fun onSaveInstanceState(outState: Bundle) = pagerAdapter.onSaveInstanceState(outState)

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_new_template -> NewTemplateDialog.show(childFragmentManager)
            R.id.action_set_default_template -> defaultTemplateKey = pagerAdapter.currentTabKey!!
            else -> return false
        }
        return true
    }

    override fun onClick(v: View) {
        childFragmentManager.fragments
                .filterIsInstance<TemplateFragment>()
                .filter { pagerAdapter.currentTabKey == it.metricsRef.parent.key }
                .also {
                    if (it.size > SINGLE_ITEM) {
                        throw IllegalStateException(
                                "Multiple fragments found with key ${it[0].metricsRef.parent.key}")
                    }

                    it[0].onClick(v)
                }
    }

    fun onTemplateCreated(key: String) {
        pagerAdapter.currentTabKey = key

        Snackbar.make(rootView, R.string.title_template_added, Snackbar.LENGTH_LONG)
                .setAction(R.string.title_set_default_template) { defaultTemplateKey = key }
                .show()
    }

    override fun onBackPressed(): Boolean =
            childFragmentManager.fragments.any { it is OnBackPressedListener && it.onBackPressed() }

    companion object {
        const val TAG = "TemplateListFragment"

        fun newInstance(templateKey: String?) =
                TemplateListFragment().apply { arguments = getTabKeyBundle(templateKey) }
    }
}


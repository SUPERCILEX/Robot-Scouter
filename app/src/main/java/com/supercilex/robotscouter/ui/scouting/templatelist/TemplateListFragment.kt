package com.supercilex.robotscouter.ui.scouting.templatelist

import android.os.Bundle
import android.support.annotation.DrawableRes
import android.support.annotation.IdRes
import android.support.design.widget.Snackbar
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.support.v7.content.res.AppCompatResources
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.github.clans.fab.FloatingActionButton
import com.github.clans.fab.FloatingActionMenu
import com.google.firebase.appindexing.FirebaseUserActions
import com.google.firebase.appindexing.builders.Actions
import com.google.firebase.auth.FirebaseAuth
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.TemplateType
import com.supercilex.robotscouter.util.data.TAB_KEY
import com.supercilex.robotscouter.util.data.defaultTemplateId
import com.supercilex.robotscouter.util.data.getTabId
import com.supercilex.robotscouter.util.data.getTabIdBundle
import com.supercilex.robotscouter.util.data.getTemplateLink
import com.supercilex.robotscouter.util.data.model.addTemplate
import com.supercilex.robotscouter.util.isSingleton
import com.supercilex.robotscouter.util.logViewTemplateEvent
import com.supercilex.robotscouter.util.ui.FragmentBase
import com.supercilex.robotscouter.util.ui.OnBackPressedListener
import com.supercilex.robotscouter.util.ui.RecyclerPoolHolder

class TemplateListFragment : FragmentBase(),
        View.OnClickListener, OnBackPressedListener, RecyclerPoolHolder,
        FirebaseAuth.AuthStateListener {
    override val recyclerPool = RecyclerView.RecycledViewPool()

    private val rootView by lazy { View.inflate(context, R.layout.fragment_template_list, null) }
    val fam: FloatingActionMenu by lazy { rootView.findViewById<FloatingActionMenu>(R.id.fab_menu) }
    private val pagerAdapter by lazy {
        val tabLayout = rootView.findViewById<TabLayout>(R.id.tabs)
        val viewPager = rootView.findViewById<ViewPager>(R.id.viewpager)
        val adapter = object : TemplatePagerAdapter(this, tabLayout) {
            override fun onDataChanged() {
                super.onDataChanged()
                if (holder.scouts.isEmpty()) {
                    fam.hideMenuButton(true)
                } else {
                    fam.showMenuButton(true)
                }
            }

            override fun onTabSelected(tab: TabLayout.Tab) {
                super.onTabSelected(tab)
                fam.close(true)
                fam.showMenuButton(true)

                currentTabId?.let {
                    logViewTemplateEvent(it)
                    FirebaseUserActions.getInstance().end(
                            Actions.newView(tab.text?.toString()!!, getTemplateLink(it)))
                }
            }
        }

        viewPager.adapter = adapter
        tabLayout.setupWithViewPager(viewPager)

        adapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        FirebaseAuth.getInstance().addAuthStateListener(this)
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

        fam.hideMenuButton(false)

        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        pagerAdapter
        handleArgs(arguments, savedInstanceState)
    }

    override fun onDestroy() {
        super.onDestroy()
        FirebaseAuth.getInstance().removeAuthStateListener(this)
    }

    fun handleArgs(args: Bundle, savedInstanceState: Bundle? = null) {
        val templateId = getTabId(args)
        if (templateId != null) {
            pagerAdapter.currentTabId = TemplateType.coerce(templateId)?.let {
                Snackbar.make(rootView,
                              R.string.title_template_added_as_default,
                              Snackbar.LENGTH_LONG)
                        .show()

                addTemplate(it).also { defaultTemplateId = it }
            } ?: templateId

            args.remove(TAB_KEY)
        } else {
            savedInstanceState?.let { pagerAdapter.currentTabId = getTabId(it) }
        }
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
            R.id.action_share -> TemplateSharer.shareTemplate(
                    activity,
                    pagerAdapter.currentTabId!!,
                    pagerAdapter.currentTab?.text?.toString()!!)
            else -> return false
        }
        return true
    }

    override fun onClick(v: View) {
        childFragmentManager.fragments
                .filterIsInstance<TemplateFragment>()
                .filter { pagerAdapter.currentTabId == it.metricsRef.parent.id }
                .also {
                    if (!it.isSingleton) {
                        throw IllegalStateException(
                                "Multiple fragments found with id ${it[0].metricsRef.parent.id}")
                    }

                    it[0].onClick(v)
                }
    }

    fun onTemplateCreated(id: String) {
        pagerAdapter.currentTabId = id

        Snackbar.make(rootView, R.string.title_template_added, Snackbar.LENGTH_LONG)
                .setAction(R.string.title_set_default_template) { defaultTemplateId = id }
                .show()
    }

    override fun onBackPressed(): Boolean =
            childFragmentManager.fragments.any { it is OnBackPressedListener && it.onBackPressed() }

    override fun onAuthStateChanged(auth: FirebaseAuth) {
        if (auth.currentUser == null) activity.finish()
    }

    companion object {
        const val TAG = "TemplateListFragment"

        fun newInstance(templateId: String?) =
                TemplateListFragment().apply { arguments = getTabIdBundle(templateId) }
    }
}

package com.supercilex.robotscouter.feature.templates

import android.os.Bundle
import android.support.annotation.DrawableRes
import android.support.annotation.IdRes
import android.support.design.widget.TabLayout
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
import com.google.firebase.auth.FirebaseAuth
import com.supercilex.robotscouter.core.data.TAB_KEY
import com.supercilex.robotscouter.core.data.defaultTemplateId
import com.supercilex.robotscouter.core.data.getTabId
import com.supercilex.robotscouter.core.data.getTabIdBundle
import com.supercilex.robotscouter.core.data.model.addTemplate
import com.supercilex.robotscouter.core.model.TemplateType
import com.supercilex.robotscouter.core.ui.FragmentBase
import com.supercilex.robotscouter.core.ui.OnBackPressedListener
import com.supercilex.robotscouter.core.ui.RecyclerPoolHolder
import com.supercilex.robotscouter.core.unsafeLazy
import kotlinx.android.synthetic.main.fragment_template_list.*
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.find

internal class TemplateListFragment : FragmentBase(),
        View.OnClickListener, OnBackPressedListener, RecyclerPoolHolder,
        FirebaseAuth.AuthStateListener {
    override val recyclerPool = RecyclerView.RecycledViewPool()

    val pagerAdapter: TemplatePagerAdapter by unsafeLazy {
        val adapter = object : TemplatePagerAdapter(this@TemplateListFragment) {
            override fun onDataChanged() {
                super.onDataChanged()
                if (currentScouts.isEmpty()) {
                    fam.hideMenuButton(true)
                } else {
                    fam.showMenuButton(true)
                }
            }

            override fun onTabSelected(tab: TabLayout.Tab) {
                super.onTabSelected(tab)
                fam.close(true)
            }
        }

        viewPager.adapter = adapter
        tabs.setupWithViewPager(viewPager)

        adapter
    }

    init {
        setHasOptionsMenu(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseAuth.getInstance().addAuthStateListener(this)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View = View.inflate(context, R.layout.fragment_template_list, null)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        fun initFab(@IdRes id: Int, @DrawableRes icon: Int) =
                view.find<FloatingActionButton>(id).let {
                    it.setOnClickListener(this)
                    it.setImageDrawable(AppCompatResources.getDrawable(it.context, icon))
                }
        initFab(R.id.addHeader, R.drawable.ic_title_colorable_24dp)
        initFab(R.id.addCheckbox, R.drawable.ic_done_colorable_24dp)
        initFab(R.id.addStopwatch, R.drawable.ic_timer_colorable_24dp)
        initFab(R.id.addNote, R.drawable.ic_note_colorable_24dp)
        initFab(R.id.addCounter, R.drawable.ic_count_colorable_24dp)
        initFab(R.id.addSpinner, R.drawable.ic_list_colorable_24dp)

        fam.hideMenuButton(false)

        pagerAdapter
        handleArgs(arguments!!, savedInstanceState)
    }

    override fun onDestroy() {
        super.onDestroy()
        FirebaseAuth.getInstance().removeAuthStateListener(this)
    }

    fun handleArgs(args: Bundle, savedInstanceState: Bundle? = null) {
        val templateId = getTabId(args)
        if (templateId != null) {
            pagerAdapter.currentTabId = TemplateType.coerce(templateId)?.let {
                longSnackbar(fam, R.string.template_added_message)
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
        activity.setSupportActionBar(toolbar)
        activity.supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) =
            inflater.inflate(R.menu.template_list_menu, menu)

    override fun onSaveInstanceState(outState: Bundle) = pagerAdapter.onSaveInstanceState(outState)

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_new_template -> NewTemplateDialog.show(childFragmentManager)
            R.id.action_share -> TemplateSharer.shareTemplate(
                    this,
                    pagerAdapter.currentTabId!!,
                    pagerAdapter.currentTab?.text?.toString()!!
            )
            else -> return false
        }
        return true
    }

    override fun onClick(v: View) {
        childFragmentManager.fragments
                .filterIsInstance<TemplateFragment>()
                .filter { pagerAdapter.currentTabId == it.dataId }
                .also { it.firstOrNull()?.onClick(v) }
    }

    fun onTemplateCreated(id: String) {
        pagerAdapter.currentTabId = id
        longSnackbar(fam, R.string.template_added_title, R.string.template_set_default_title) {
            defaultTemplateId = id
        }
    }

    override fun onBackPressed(): Boolean =
            childFragmentManager.fragments.any { it is OnBackPressedListener && it.onBackPressed() }

    override fun onAuthStateChanged(auth: FirebaseAuth) {
        if (auth.currentUser == null) requireActivity().finish()
    }

    companion object {
        const val TAG = "TemplateListFragment"

        fun newInstance(templateId: String?) =
                TemplateListFragment().apply { arguments = getTabIdBundle(templateId) }
    }
}

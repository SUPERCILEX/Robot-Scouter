package com.supercilex.robotscouter.feature.templates

import android.animation.FloatEvaluator
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Guideline
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.LayoutParams
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.supercilex.robotscouter.Bridge
import com.supercilex.robotscouter.Refreshable
import com.supercilex.robotscouter.SignInResolver
import com.supercilex.robotscouter.TemplateListFragmentCompanion
import com.supercilex.robotscouter.TemplateListFragmentCompanion.Companion.TAG
import com.supercilex.robotscouter.core.data.TAB_KEY
import com.supercilex.robotscouter.core.data.defaultTemplateId
import com.supercilex.robotscouter.core.data.getTabId
import com.supercilex.robotscouter.core.data.getTabIdBundle
import com.supercilex.robotscouter.core.data.isSignedIn
import com.supercilex.robotscouter.core.data.model.addTemplate
import com.supercilex.robotscouter.core.model.TemplateType
import com.supercilex.robotscouter.core.ui.FragmentBase
import com.supercilex.robotscouter.core.ui.LifecycleAwareLazy
import com.supercilex.robotscouter.core.ui.RecyclerPoolHolder
import com.supercilex.robotscouter.core.ui.animateChange
import com.supercilex.robotscouter.core.ui.isInTabletMode
import com.supercilex.robotscouter.core.ui.longSnackbar
import com.supercilex.robotscouter.core.ui.onDestroy
import com.supercilex.robotscouter.core.unsafeLazy
import kotlinx.android.synthetic.main.fragment_template_list.*
import com.supercilex.robotscouter.R as RC

@Bridge
internal class TemplateListFragment : FragmentBase(R.layout.fragment_template_list), Refreshable,
        View.OnClickListener, RecyclerPoolHolder {
    override val recyclerPool by LifecycleAwareLazy { RecyclerView.RecycledViewPool() }

    val pagerAdapter by unsafeLazy {
        object : TemplatePagerAdapter(this@TemplateListFragment) {
            override fun onDataChanged() {
                super.onDataChanged()
                if (currentScouts.isEmpty()) {
                    fab.hide()
                } else {
                    fab.show()
                }
            }
        }
    }
    val fab: FloatingActionButton by unsafeLazy {
        requireActivity().findViewById(RC.id.fab)
    }
    private val appBar: AppBarLayout by unsafeLazy {
        requireActivity().findViewById(RC.id.appBar)
    }
    private val tabs by LifecycleAwareLazy {
        val tabs = TabLayout(ContextThemeWrapper(
                requireContext(),
                RC.style.ThemeOverlay_AppCompat_Dark_ActionBar
        )).apply {
            id = R.id.tabs
            tabMode = TabLayout.MODE_SCROLLABLE
        }
        appBar.addView(
                tabs, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            scrollFlags = LayoutParams.SCROLL_FLAG_ENTER_ALWAYS_COLLAPSED
        })
        tabs
    } onDestroy {
        appBar.removeView(it)
    }
    private val homeDivider: Guideline? by unsafeLazy {
        val activity = requireActivity()
        if (activity.isInTabletMode()) activity.findViewById<Guideline>(RC.id.guideline) else null
    }

    private var savedState: Bundle? = null

    init {
        setHasOptionsMenu(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedState = savedInstanceState
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        animateContainerMorph(2f / 3)

        tabs // Force init
        viewPager.adapter = pagerAdapter
        tabs.setupWithViewPager(viewPager)
        fab.setOnClickListener(this)

        handleArgs(arguments, savedInstanceState)
    }

    override fun refresh() {
        childFragmentManager.fragments
                .filterIsInstance<TemplateFragment>()
                .singleOrNull { pagerAdapter.currentTabId == it.dataId }
                ?.refresh()
    }

    override fun onStop() {
        super.onStop()
        // This has to be done in onStop so fragment transactions from the view pager can be
        // committed. Only reset the adapter if the user is switching destinations.
        if (isDetached) pagerAdapter.reset()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewPager.adapter = null
        animateContainerMorph(1f / 3)
        fab.apply {
            setOnClickListener(null)
            hide()
            isVisible = false // TODO hack: don't animate
        }
    }

    fun handleArgs(args: Bundle) {
        if (view == null) {
            arguments = (arguments ?: Bundle()).apply { putAll(args) }
        } else {
            handleArgs(args, null)
        }
    }

    private fun handleArgs(args: Bundle?, savedInstanceState: Bundle?) {
        val templateId = getTabId(args)
        if (templateId == null) {
            getTabId(savedInstanceState)?.let { pagerAdapter.currentTabId = it }
        } else {
            pagerAdapter.currentTabId = TemplateType.coerce(templateId)?.let {
                viewPager.longSnackbar(R.string.template_added_message)
                addTemplate(it).also { defaultTemplateId = it }
            } ?: templateId

            args?.remove(TAB_KEY)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) =
            inflater.inflate(R.menu.template_list_menu, menu)

    override fun onSaveInstanceState(outState: Bundle) =
            outState.putAll(getTabIdBundle(pagerAdapter.currentTabId ?: getTabId(savedState)))

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (!isSignedIn) {
            (activity as SignInResolver).showSignInResolution()
            return false
        }

        when (item.itemId) {
            R.id.action_new_template -> NewTemplateDialog.show(childFragmentManager)
            R.id.action_share -> TemplateSharer.shareTemplate(
                    this,
                    checkNotNull(pagerAdapter.currentTabId),
                    checkNotNull(pagerAdapter.currentTab?.text?.toString())
            )
            else -> return false
        }
        return true
    }

    override fun onClick(v: View) {
        if (v.id == RC.id.fab) {
            AddMetricDialog.show(childFragmentManager)
        } else {
            childFragmentManager.fragments
                    .filterIsInstance<TemplateFragment>()
                    .singleOrNull { pagerAdapter.currentTabId == it.dataId }
                    ?.onClick(v)
        }
    }

    fun onTemplateCreated(id: String) {
        pagerAdapter.currentTabId = id
        viewPager.longSnackbar(
                R.string.template_added_title,
                RC.string.template_set_default_title
        ) { defaultTemplateId = id }
    }

    private fun animateContainerMorph(new: Float) {
        val div = homeDivider ?: return
        val current = (div.layoutParams as ConstraintLayout.LayoutParams).guidePercent
        animateChange(FloatEvaluator(), current, new) {
            div.setGuidelinePercent(it.animatedValue as Float)
        }
    }

    companion object : TemplateListFragmentCompanion {
        override fun getInstance(
                manager: FragmentManager,
                args: Bundle?
        ): TemplateListFragment {
            val instance = manager.findFragmentByTag(TAG) as TemplateListFragment?
                    ?: TemplateListFragment()
            args?.let { instance.handleArgs(args) }
            return instance
        }
    }
}

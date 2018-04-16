package com.supercilex.robotscouter.shared.scouting

import android.arch.lifecycle.DefaultLifecycleObserver
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.view.PagerAdapter
import android.view.View
import android.widget.LinearLayout
import com.google.firebase.firestore.CollectionReference
import com.supercilex.robotscouter.core.data.ChangeEventListenerBase
import com.supercilex.robotscouter.core.data.ListenerRegistrationLifecycleOwner
import com.supercilex.robotscouter.core.data.getTabIdBundle
import com.supercilex.robotscouter.core.data.isPolynomial
import com.supercilex.robotscouter.core.data.mainHandler
import com.supercilex.robotscouter.core.data.model.ScoutsHolder
import com.supercilex.robotscouter.core.model.Scout
import com.supercilex.robotscouter.core.ui.Saveable
import com.supercilex.robotscouter.core.ui.animatePopReveal
import com.supercilex.robotscouter.core.ui.setOnLongClickListenerCompat
import com.supercilex.robotscouter.shared.MovableFragmentStatePagerAdapter
import kotlinx.android.extensions.LayoutContainer
import org.jetbrains.anko.find

abstract class TabPagerAdapterBase(
        protected val fragment: Fragment,
        private val dataRef: CollectionReference
) : MovableFragmentStatePagerAdapter(fragment.childFragmentManager), LayoutContainer,
        Saveable,
        TabLayout.OnTabSelectedListener, View.OnLongClickListener, DefaultLifecycleObserver,
        ChangeEventListenerBase {
    @get:StringRes protected abstract val editTabNameRes: Int
    final override val containerView = fragment.view!!
    private val tabs = containerView.find<TabLayout>(R.id.tabs)
    private val noTabsHint = containerView.find<View>(R.id.noTabsHint)

    val holder = ViewModelProviders.of(fragment).get(ScoutsHolder::class.java)
    private var oldScouts: List<Scout> = emptyList()
    protected var currentScouts: List<Scout> = emptyList()

    var currentTabId: String? = null
        set(value) {
            field = value
            currentScouts.indexOfFirst { it.id == field }.let { if (it != -1) selectTab(it) }
        }
    val currentTab: TabLayout.Tab?
        get() = tabs.getTabAt(currentScouts.indexOfFirst { it.id == currentTabId })

    fun init() {
        fragment.lifecycle.addObserver(this)
        ListenerRegistrationLifecycleOwner.lifecycle.addObserver(this)
    }

    override fun getCount() = currentScouts.size

    override fun getItemPosition(f: Any): Int {
        val id = (f as MetricListFragment).metricsRef.parent!!.id
        val position = currentScouts.indexOfFirst { it.id == id }
        return if (position == -1) PagerAdapter.POSITION_NONE else position
    }

    override fun getItemId(position: Int) = currentScouts[position].id

    override fun onTabSelected(tab: TabLayout.Tab) {
        currentTabId = currentScouts[tab.position].id
    }

    override fun onDataChanged() {
        currentScouts = holder.scouts.toList()
        if (currentScouts.isNotEmpty() && currentScouts.size == oldScouts.size) {
            if (currentScouts == oldScouts) {
                // This will occur when re-establishing a connection to the database
                return
            }

            // Check to see if this update is just a name change
            val newScoutsWithOldNames = currentScouts.mapIndexed { index, scout ->
                scout.copy(name = oldScouts[index].name)
            }
            if (newScoutsWithOldNames == oldScouts) {
                updateTabNames()

                oldScouts = currentScouts.toList()
                return
            }
        }

        val prevTabId = currentTabId

        noTabsHint.animatePopReveal(currentScouts.isEmpty())

        tabs.removeOnTabSelectedListener(this)
        notifyDataSetChanged()
        tabs.addOnTabSelectedListener(this)

        updateTabNames()

        if (currentScouts.isNotEmpty()) {
            if (prevTabId.isNullOrBlank()) {
                currentTabId = currentScouts.first().id
            } else {
                currentScouts.find { it.id == prevTabId }?.let {
                    selectTab(currentScouts.indexOfFirst { it.id == currentTabId })
                } ?: run {
                    val index = oldScouts.indexOfFirst { it.id == prevTabId }
                    currentTabId = if (oldScouts.isPolynomial) {
                        (if (oldScouts.lastIndex > index) {
                            oldScouts[index + 1]
                        } else {
                            oldScouts[index - 1]
                        }).id
                    } else {
                        null
                    }
                }
            }
        }

        oldScouts = currentScouts.toList()
    }

    private fun updateTabNames() {
        (0 until tabs.tabCount).map {
            tabs.getTabAt(it)!!
        }.forEachIndexed { index, tab ->
            tab.text = currentScouts[index].name ?: getPageTitle(index)

            val tabView = (tabs.getChildAt(0) as LinearLayout).getChildAt(index)
            tabView.setOnLongClickListenerCompat(this@TabPagerAdapterBase)
            tabView.id = index
        }
    }

    override fun onSaveInstanceState(outState: Bundle) =
            outState.putAll(getTabIdBundle(currentTabId))

    private fun selectTab(index: Int) {
        val select: () -> Unit = { tabs.getTabAt(index)?.select() }

        // Select the tab twice:
        // 1. Ensure we don't wastefully load other templates if the selected one is somewhere in
        //    the middle and the post takes too long to change the selection.
        // 2. If the tabs are updated, we'll lose our position while updating so posting ensures
        //    the selection happens after the adapter has processed the notify call.
        select()
        mainHandler.post(select)
    }

    override fun onStart(owner: LifecycleOwner) {
        if (owner === fragment) {
            holder.scouts.addChangeEventListener(this)
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        if (owner === fragment) {
            holder.scouts.removeChangeEventListener(this)
        } else if (owner === ListenerRegistrationLifecycleOwner) {
            oldScouts = emptyList()
            currentScouts = emptyList()
            notifyDataSetChanged()
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        ListenerRegistrationLifecycleOwner.lifecycle.removeObserver(this)
    }

    override fun onLongClick(v: View): Boolean {
        TabNameDialog.show(
                fragment.childFragmentManager,
                dataRef.document(currentScouts[v.id].id),
                editTabNameRes,
                tabs.getTabAt(v.id)!!.text!!.toString()
        )
        return true
    }

    override fun onTabUnselected(tab: TabLayout.Tab) = Unit

    override fun onTabReselected(tab: TabLayout.Tab) = Unit
}

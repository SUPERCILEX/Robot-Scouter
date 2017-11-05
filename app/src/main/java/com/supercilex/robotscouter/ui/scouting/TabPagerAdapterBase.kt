package com.supercilex.robotscouter.ui.scouting

import android.arch.core.executor.ArchTaskExecutor
import android.arch.lifecycle.DefaultLifecycleObserver
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.PagerAdapter
import android.text.TextUtils
import android.view.View
import android.widget.LinearLayout
import com.google.firebase.firestore.CollectionReference
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Scout
import com.supercilex.robotscouter.util.data.ChangeEventListenerBase
import com.supercilex.robotscouter.util.data.ListenerRegistrationLifecycleOwner
import com.supercilex.robotscouter.util.data.getTabIdBundle
import com.supercilex.robotscouter.util.data.model.ScoutsHolder
import com.supercilex.robotscouter.util.isPolynomial
import com.supercilex.robotscouter.util.ui.animatePopReveal
import kotterknife.bindView

abstract class TabPagerAdapterBase(
        protected val fragment: Fragment,
        private val tabLayout: TabLayout,
        protected val dataRef: CollectionReference
) : FragmentStatePagerAdapter(fragment.childFragmentManager),
        TabLayout.OnTabSelectedListener, View.OnLongClickListener, DefaultLifecycleObserver,
        ChangeEventListenerBase {
    @get:StringRes protected abstract val editTabNameRes: Int
    private val noContentHint: View by fragment.bindView(R.id.no_content_hint)

    protected val holder: ScoutsHolder = ViewModelProviders.of(fragment).get(ScoutsHolder::class.java)
    protected var oldScouts: List<Scout> = emptyList()
    private var currentScouts: List<Scout> = emptyList()

    var currentTabId: String? = null
        set(value) {
            field = value
            currentScouts.indexOfFirst { it.id == field }.let { if (it != -1) selectTab(it) }
        }
    val currentTab: TabLayout.Tab?
        get() = tabLayout.getTabAt(currentScouts.indexOfFirst { it.id == currentTabId })

    init {
        fragment.lifecycle.addObserver(this)
        ListenerRegistrationLifecycleOwner.lifecycle.addObserver(this)
    }

    override fun getCount() = currentScouts.size

    override fun getItemPosition(any: Any) = PagerAdapter.POSITION_NONE

    override fun onTabSelected(tab: TabLayout.Tab) {
        currentTabId = currentScouts[tab.position].id
    }

    override fun onDataChanged() {
        currentScouts = ArrayList(holder.scouts)
        if (currentScouts.isNotEmpty() && currentScouts.size == oldScouts.size) {
            if (currentScouts == oldScouts) {
                // This will occur when re-establishing a connection to the database
                return
            }

            val newScoutsWithOldNames = currentScouts.mapIndexed { index, scout ->
                scout.copy(name = oldScouts[index].name)
            }
            if (newScoutsWithOldNames == oldScouts) {
                updateTabNames()

                oldScouts = ArrayList(currentScouts)
                return
            }
        }

        val prevTabId = currentTabId

        noContentHint.animatePopReveal(currentScouts.isEmpty())

        tabLayout.removeOnTabSelectedListener(this)
        notifyDataSetChanged()
        tabLayout.addOnTabSelectedListener(this)

        updateTabNames()

        if (currentScouts.isNotEmpty()) {
            if (TextUtils.isEmpty(prevTabId)) {
                currentTabId = currentScouts[0].id
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

        oldScouts = ArrayList(currentScouts)
    }

    private fun updateTabNames() {
        (0 until tabLayout.tabCount).map {
            tabLayout.getTabAt(it)!!
        }.forEachIndexed { index, tab ->
            tab.text = currentScouts[index].name ?: getPageTitle(index)

            val tabView = (tabLayout.getChildAt(0) as LinearLayout).getChildAt(index)
            tabView.setOnLongClickListener(this@TabPagerAdapterBase)
            tabView.id = index
        }
    }

    fun onSaveInstanceState(outState: Bundle) = outState.putAll(getTabIdBundle(currentTabId))

    private fun selectTab(index: Int) {
        ArchTaskExecutor.getInstance().postToMainThread {
            tabLayout.getTabAt(index)?.select()
        }
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
                tabLayout.getTabAt(v.id)!!.text!!.toString()
        )
        return true
    }

    override fun onTabUnselected(tab: TabLayout.Tab) = Unit

    override fun onTabReselected(tab: TabLayout.Tab) = Unit
}

package com.supercilex.robotscouter.ui.scout

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleFragment
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.Observer
import android.arch.lifecycle.OnLifecycleEvent
import android.arch.lifecycle.ViewModelProviders
import android.support.design.widget.TabLayout
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.PagerAdapter
import android.text.TextUtils
import android.view.View
import android.widget.LinearLayout
import com.google.firebase.crash.FirebaseCrash
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.data.util.deleteScout
import com.supercilex.robotscouter.util.FIREBASE_NAME
import com.supercilex.robotscouter.util.FIREBASE_SCOUTS
import com.supercilex.robotscouter.util.SINGLE_ITEM
import com.supercilex.robotscouter.util.isOffline
import java.util.ArrayList

class ScoutPagerAdapter(private val fragment: LifecycleFragment,
                        private val appBarViewHolder: AppBarViewHolderBase,
                        private val tabLayout: TabLayout,
                        private val team: Team,
                        var currentScoutKey: String?) :
        FragmentStatePagerAdapter(fragment.childFragmentManager),
        Observer<List<String>>, TabLayout.OnTabSelectedListener, View.OnLongClickListener, LifecycleObserver {
    private val tabNameListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val tabIndex = keys.indexOf(snapshot.ref.parent.key)
            val tab = tabLayout.getTabAt(tabIndex)

            tab!!.text = if (snapshot.value == null)
                getPageTitle(tabIndex)
            else
                snapshot.getValue(String::class.java)

            val tabView = (tabLayout.getChildAt(0) as LinearLayout).getChildAt(tabIndex)
            tabView.setOnLongClickListener(this@ScoutPagerAdapter)
            tabView.id = tabIndex
        }

        override fun onCancelled(error: DatabaseError) = FirebaseCrash.report(error.toException())
    }

    private val holder: ScoutKeysHolder = ViewModelProviders.of(fragment).get(ScoutKeysHolder::class.java)
    private var keys: List<String> = ArrayList()

    init {
        holder.init(team.key)
        holder.keysListener.observe(fragment, this)

        fragment.lifecycle.addObserver(this)
    }

    override fun getItem(position: Int) = ScoutFragment.newInstance(keys[position])

    override fun getCount() = keys.size

    override fun getItemPosition(any: Any?) = PagerAdapter.POSITION_NONE

    override fun getPageTitle(position: Int): String =
            fragment.getString(R.string.title_scout_tab, count - position)

    fun onScoutDeleted() {
        val index = keys.indexOf(currentScoutKey)
        var newKey: String? = null
        if (keys.size > SINGLE_ITEM) {
            newKey = if (keys.size - 1 > index) keys[index + 1] else keys[index - 1]
        }
        deleteScout(holder.ref.key, currentScoutKey!!)
        currentScoutKey = newKey
    }

    override fun onTabSelected(tab: TabLayout.Tab) {
        currentScoutKey = keys[tab.position]
    }

    override fun onChanged(newKeys: List<String>?) {
        removeNameListeners()
        val hadScouts = !keys.isEmpty()

        keys = newKeys!!

        for (key in keys) getTabNameRef(key).addValueEventListener(tabNameListener)
        if (hadScouts && keys.isEmpty() && !isOffline() && fragment.isResumed) {
            ShouldDeleteTeamDialog.show(fragment.childFragmentManager, team)
        }
        fragment.view!!.findViewById<View>(R.id.no_content_hint).visibility =
                if (keys.isEmpty()) View.VISIBLE else View.GONE
        appBarViewHolder.setDeleteScoutMenuItemVisible(!keys.isEmpty())

        tabLayout.removeOnTabSelectedListener(this)
        notifyDataSetChanged()
        tabLayout.addOnTabSelectedListener(this)

        if (!keys.isEmpty()) {
            if (TextUtils.isEmpty(currentScoutKey)) {
                selectTab(0)
                currentScoutKey = keys[0]
            } else {
                selectTab(keys.indexOf(currentScoutKey))
            }
        }
    }

    private fun selectTab(index: Int) = tabLayout.getTabAt(index)?.select()

    private fun getTabNameRef(key: String) = FIREBASE_SCOUTS.child(key).child(FIREBASE_NAME)

    @Suppress("unused")
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun cleanup() {
        fragment.lifecycle.removeObserver(this)
        removeNameListeners()
        RobotScouter.getRefWatcher(fragment.activity).watch(this)
    }

    private fun removeNameListeners() {
        for (key in keys) getTabNameRef(key).removeEventListener(tabNameListener)
    }

    override fun onLongClick(v: View): Boolean {
        ScoutNameDialog.show(
                fragment.childFragmentManager,
                FIREBASE_SCOUTS.child(keys[v.id]).child(FIREBASE_NAME),
                tabLayout.getTabAt(v.id)!!.text!!.toString())
        return true
    }

    override fun onTabUnselected(tab: TabLayout.Tab) = Unit

    override fun onTabReselected(tab: TabLayout.Tab) = Unit
}

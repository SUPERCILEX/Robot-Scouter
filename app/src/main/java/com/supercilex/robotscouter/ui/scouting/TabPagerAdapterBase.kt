package com.supercilex.robotscouter.ui.scouting

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleFragment
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.Observer
import android.arch.lifecycle.OnLifecycleEvent
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.PagerAdapter
import android.text.TextUtils
import android.view.View
import android.widget.LinearLayout
import com.google.firebase.crash.FirebaseCrash
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.util.FIREBASE_NAME
import com.supercilex.robotscouter.util.data.getTabKeyBundle
import java.util.ArrayList

abstract class TabPagerAdapterBase(protected val fragment: LifecycleFragment,
                                   private val tabLayout: TabLayout,
                                   keysRef: DatabaseReference,
                                   private val dataRef: DatabaseReference) :
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
            tabView.setOnLongClickListener(this@TabPagerAdapterBase)
            tabView.id = tabIndex
        }

        override fun onCancelled(error: DatabaseError) = FirebaseCrash.report(error.toException())
    }

    var currentTabKey: String? = null
        set(value) {
            field = value
            keys.indexOf(field).let { if (it != -1) selectTab(it) }
        }

    protected val holder: TabKeysHolder = ViewModelProviders.of(fragment).get(TabKeysHolder::class.java)
    protected var keys: List<String> = ArrayList()

    init {
        holder.init(keysRef)
    }

    protected fun init() {
        holder.keysListener.observe(fragment, this)
        fragment.lifecycle.addObserver(this)
    }

    override fun getCount() = keys.size

    override fun getItemPosition(any: Any?) = PagerAdapter.POSITION_NONE

    override fun onTabSelected(tab: TabLayout.Tab) {
        currentTabKey = keys[tab.position]
    }

    override fun onChanged(newKeys: List<String>?) {
        removeNameListeners()

        keys = newKeys!!

        for (key in keys) getTabNameRef(key).addValueEventListener(tabNameListener)
        fragment.view!!.findViewById<View>(R.id.no_content_hint).visibility =
                if (keys.isEmpty()) View.VISIBLE else View.GONE

        tabLayout.removeOnTabSelectedListener(this)
        notifyDataSetChanged()
        tabLayout.addOnTabSelectedListener(this)

        if (!keys.isEmpty()) {
            if (TextUtils.isEmpty(currentTabKey)) {
                currentTabKey = keys[0]
            } else {
                selectTab(keys.indexOf(currentTabKey))
            }
        }
    }

    fun onSaveInstanceState(outState: Bundle) {
        outState.putAll(getTabKeyBundle(currentTabKey))
    }

    private fun selectTab(index: Int) = tabLayout.getTabAt(index)?.select()

    private fun getTabNameRef(key: String) = dataRef.child(key).child(FIREBASE_NAME)

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
        TabNameDialog.show(
                fragment.childFragmentManager,
                dataRef.child(keys[v.id]).child(FIREBASE_NAME),
                tabLayout.getTabAt(v.id)!!.text!!.toString())
        return true
    }

    override fun onTabUnselected(tab: TabLayout.Tab) = Unit

    override fun onTabReselected(tab: TabLayout.Tab) = Unit
}

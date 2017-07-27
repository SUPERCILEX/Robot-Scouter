package com.supercilex.robotscouter.ui.scouting

import android.arch.lifecycle.LifecycleFragment
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.database.DatabaseReference
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.RobotScouter

abstract class MetricListFragment : LifecycleFragment() {
    protected val holder: MetricListHolder by lazy { ViewModelProviders.of(this).get(MetricListHolder::class.java) }
    protected abstract val metricsRef: DatabaseReference

    protected val recyclerView: RecyclerView by lazy {
        View.inflate(context, R.layout.fragment_metric_list, null) as RecyclerView
    }
    protected abstract val adapter: MetricListAdapterBase
    protected val manager by lazy { LinearLayoutManager(context) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        holder.init(metricsRef)
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        recyclerView.layoutManager = manager
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = adapter

        return recyclerView
    }

    override fun onStop() {
        super.onStop()
        recyclerView.clearFocus()
    }

    override fun onDestroy() {
        super.onDestroy()
        RobotScouter.getRefWatcher(activity).watch(this)
    }
}

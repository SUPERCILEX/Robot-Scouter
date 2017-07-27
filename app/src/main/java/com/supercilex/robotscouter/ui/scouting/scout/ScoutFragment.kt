package com.supercilex.robotscouter.ui.scouting.scout

import android.arch.lifecycle.LifecycleFragment
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.util.data.model.getScoutKey
import com.supercilex.robotscouter.util.data.model.getScoutKeyBundle

class ScoutFragment : LifecycleFragment() {
    private val holder: MetricsHolder by lazy { ViewModelProviders.of(this).get(MetricsHolder::class.java) }

    private val recyclerView: RecyclerView by lazy {
        View.inflate(context, R.layout.fragment_scout, null) as RecyclerView
    }
    private val adapter: ScoutAdapter by lazy {
        ScoutAdapter(holder.metrics, childFragmentManager, recyclerView, this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        holder.init(getScoutKey(arguments)!!)
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        recyclerView.layoutManager = LinearLayoutManager(context)
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

    companion object {
        fun newInstance(scoutKey: String): ScoutFragment =
                ScoutFragment().apply { arguments = getScoutKeyBundle(scoutKey) }
    }
}

package com.supercilex.robotscouter.ui.scout

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.RobotScouter

import com.supercilex.robotscouter.data.util.getScoutKey
import com.supercilex.robotscouter.data.util.getScoutKeyBundle
import com.supercilex.robotscouter.data.util.getScoutMetricsRef
import com.supercilex.robotscouter.util.restoreRecyclerViewState
import com.supercilex.robotscouter.util.saveRecyclerViewState

class ScoutFragment : Fragment() {
    private val scoutKey: String by lazy { getScoutKey(arguments)!! }

    private val recyclerView: RecyclerView by lazy {
        View.inflate(context, R.layout.fragment_scout, null) as RecyclerView
    }
    private val manager: RecyclerView.LayoutManager by lazy { LinearLayoutManager(context) }
    private val adapter: ScoutAdapter by lazy {
        ScoutAdapter(getScoutMetricsRef(scoutKey), childFragmentManager, recyclerView)
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        recyclerView.layoutManager = manager
        recyclerView.setHasFixedSize(true)

        recyclerView.adapter = adapter
        restoreRecyclerViewState(savedInstanceState, manager)

        return recyclerView
    }

    override fun onSaveInstanceState(outState: Bundle) {
        saveRecyclerViewState(outState, manager)
        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        super.onPause()
        recyclerView.clearFocus()
    }

    override fun onDestroy() {
        super.onDestroy()
        adapter.cleanup()
        RobotScouter.getRefWatcher(activity).watch(this)
    }

    companion object {
        fun newInstance(scoutKey: String): ScoutFragment =
                ScoutFragment().apply { arguments = getScoutKeyBundle(scoutKey) }
    }
}

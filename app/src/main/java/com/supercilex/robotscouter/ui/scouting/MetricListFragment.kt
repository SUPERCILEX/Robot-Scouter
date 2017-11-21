package com.supercilex.robotscouter.ui.scouting

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.firestore.CollectionReference
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.util.LateinitVal
import com.supercilex.robotscouter.util.ui.FragmentBase
import com.supercilex.robotscouter.util.ui.SavedStateAdapter
import com.supercilex.robotscouter.util.unsafeLazy
import kotterknife.bindView

abstract class MetricListFragment : FragmentBase() {
    protected val holder: MetricListHolder by unsafeLazy {
        ViewModelProviders.of(this).get(MetricListHolder::class.java)
    }
    abstract val metricsRef: CollectionReference

    protected val recyclerView: RecyclerView by bindView(R.id.list)
    protected var adapter: SavedStateAdapter<*, *> by LateinitVal()
        private set

    /**
     * Hack to ensure the recycler adapter state is saved.
     *
     * We have to clear our pager adapter of data when the database listeners a killed to prevent
     * adapter inconsistencies. Sadly, this means onSaveInstanceState methods are going to be called
     * twice which creates invalid saved states. This property serves as a temporary place to store
     * the original saved state.
     */
    private var tmpSavedAdapterState: Bundle? = null

    init {
        setHasOptionsMenu(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        holder.init(metricsRef)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View = View.inflate(context, R.layout.fragment_metric_list, null)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.setHasFixedSize(true)
        adapter = onCreateRecyclerAdapter(savedInstanceState)
        recyclerView.adapter = adapter

        tmpSavedAdapterState = null
    }

    abstract fun onCreateRecyclerAdapter(savedInstanceState: Bundle?): SavedStateAdapter<*, *>

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putAll(tmpSavedAdapterState ?: adapter.onSaveInstanceState().also {
            tmpSavedAdapterState = it
        })
    }

    override fun onStop() {
        super.onStop()
        recyclerView.clearFocus()
    }
}

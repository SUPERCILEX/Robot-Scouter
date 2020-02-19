package com.supercilex.robotscouter.shared.scouting

import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.CollectionReference
import com.supercilex.robotscouter.core.ui.FragmentBase
import com.supercilex.robotscouter.core.ui.LifecycleAwareLazy
import com.supercilex.robotscouter.core.ui.SavedStateAdapter

abstract class MetricListFragment(@LayoutRes contentLayoutId: Int) : FragmentBase(contentLayoutId) {
    protected val holder by viewModels<MetricListHolder>()
    abstract val metricsRef: CollectionReference
    abstract val dataId: String

    protected val metricsView: RecyclerView by LifecycleAwareLazy {
        requireView().findViewById(R.id.metrics)
    }
    protected var adapter: SavedStateAdapter<*, *> by LifecycleAwareLazy()
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        metricsView.layoutManager = LinearLayoutManager(context)
        metricsView.setHasFixedSize(true)
        adapter = onCreateRecyclerAdapter(savedInstanceState)
        metricsView.adapter = adapter

        tmpSavedAdapterState = null
    }

    abstract fun onCreateRecyclerAdapter(savedInstanceState: Bundle?): SavedStateAdapter<*, *>

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putAll(tmpSavedAdapterState ?: run {
            if (view != null) adapter.onSaveInstanceState(outState)
            tmpSavedAdapterState = outState
            outState
        })
    }

    override fun onStop() {
        super.onStop()
        metricsView.clearFocus()
    }
}

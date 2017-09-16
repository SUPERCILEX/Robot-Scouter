package com.supercilex.robotscouter.ui.scouting.templatelist

import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.github.clans.fab.FloatingActionMenu
import com.google.firebase.firestore.CollectionReference
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Metric
import com.supercilex.robotscouter.ui.scouting.MetricListFragment
import com.supercilex.robotscouter.util.data.defaultTemplateId
import com.supercilex.robotscouter.util.data.delete
import com.supercilex.robotscouter.util.data.getTabId
import com.supercilex.robotscouter.util.data.getTabIdBundle
import com.supercilex.robotscouter.util.data.model.getTemplateMetricsRef
import com.supercilex.robotscouter.util.ui.OnBackPressedListener

class TemplateFragment : MetricListFragment(), View.OnClickListener, OnBackPressedListener {
    public override val metricsRef: CollectionReference by lazy {
        getTemplateMetricsRef(getTabId(arguments)!!)
    }

    override val adapter by lazy {
        TemplateAdapter(
                holder.metrics,
                childFragmentManager,
                recyclerView,
                this,
                itemTouchCallback)
    }
    private val itemTouchCallback by lazy { TemplateItemTouchCallback<Metric<*>>(recyclerView) }
    private val fam: FloatingActionMenu by lazy { (parentFragment as TemplateListFragment).fam }

    private var hasAddedItem: Boolean = false

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        val itemTouchHelper = ItemTouchHelper(itemTouchCallback)
        itemTouchCallback.itemTouchHelper = itemTouchHelper
        itemTouchCallback.adapter = adapter
        itemTouchHelper.attachToRecyclerView(recyclerView)

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0) {
                    // User scrolled down -> hide the FAB
                    fam.hideMenuButton(true)
                } else if (dy < 0) {
                    fam.showMenuButton(true)
                } else if (hasAddedItem &&
                        (manager.findFirstCompletelyVisibleItemPosition() != 0
                                || manager.findLastCompletelyVisibleItemPosition() != adapter.itemCount - 1)) {
                    fam.hideMenuButton(true)
                }

                hasAddedItem = false
            }
        })
        // This lets us close the fam when the recyclerview it touched
        recyclerView.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                fam.close(true)
                return false
            }
        })

        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) =
            inflater.inflate(R.menu.template_options, menu)

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            R.id.action_set_default_template -> defaultTemplateId = metricsRef.parent.id
            R.id.action_delete_template -> {
                recyclerView.clearFocus()
                DeleteTemplateDialog.show(childFragmentManager, metricsRef.parent)
            }
            R.id.action_remove_metrics -> {
                TODO("Add back undo SnackBar")
                metricsRef.delete()
            }
            else -> return false
        }
        return true
    }

    override fun onClick(v: View) {
        val id = v.id

        val position = adapter.itemCount
        val metricRef = metricsRef.document()
        when (id) {
            R.id.add_checkbox -> metricRef.set(Metric.Boolean(position = position))
            R.id.add_counter -> metricRef.set(Metric.Number(position = position))
            R.id.add_stopwatch -> metricRef.set(Metric.Stopwatch(position = position))
            R.id.add_note -> metricRef.set(Metric.Text(position = position))
            R.id.add_spinner -> {
                metricRef.set(Metric.List(position = position))
            }
            R.id.add_header -> metricRef.set(Metric.Header(position = position))
            else -> throw IllegalStateException("Unknown view id: $id")
        }

        itemTouchCallback.addItemToScrollQueue(adapter.itemCount)
        fam.close(true)
        hasAddedItem = true
    }

    override fun onBackPressed(): Boolean = if (fam.isOpened) {
        fam.close(true); true
    } else {
        false
    }

    companion object {
        fun newInstance(templateId: String) =
                TemplateFragment().apply { arguments = getTabIdBundle(templateId) }
    }
}

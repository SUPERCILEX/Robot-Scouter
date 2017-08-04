package com.supercilex.robotscouter.ui.scouting.template

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
import com.google.firebase.database.DatabaseReference
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Metric
import com.supercilex.robotscouter.ui.scouting.MetricListFragment
import com.supercilex.robotscouter.ui.teamlist.OnBackPressedListener
import com.supercilex.robotscouter.util.FIREBASE_TEMPLATES
import com.supercilex.robotscouter.util.FIREBASE_VALUE
import com.supercilex.robotscouter.util.data.getTabKey
import com.supercilex.robotscouter.util.data.getTabKeyBundle
import com.supercilex.robotscouter.util.data.model.getTemplateMetricsRef
import com.supercilex.robotscouter.util.data.model.parseTeam
import com.supercilex.robotscouter.util.ui.getHighestIntPriority

class TemplateFragment : MetricListFragment(), View.OnClickListener, OnBackPressedListener {
    public override val metricsRef: DatabaseReference by lazy {
        getTemplateMetricsRef(getTabKey(arguments)!!)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

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
            R.id.action_reset_template_all, R.id.action_reset_template_team -> {
                recyclerView.clearFocus()
                ResetTemplateDialog.show(
                        childFragmentManager,
                        parseTeam(arguments),
                        id == R.id.action_reset_template_all)
            }
            R.id.action_remove_metrics -> {
                RemoveAllMetricsDialog.show(
                        childFragmentManager, FIREBASE_TEMPLATES.child(metricsRef.parent.key))
            }
            else -> return false
        }
        return true
    }

    override fun onClick(v: View) {
        val id = v.id

        val priority = getHighestIntPriority(adapter.snapshots) + 1
        val metricRef = metricsRef.push()
        when (id) {
            R.id.add_checkbox -> metricRef.setValue(Metric.Boolean(), priority)
            R.id.add_counter -> metricRef.setValue(Metric.Number(), priority)
            R.id.add_stopwatch -> metricRef.setValue(Metric.Stopwatch(), priority)
            R.id.add_note -> metricRef.setValue(Metric.Text(), priority)
            R.id.add_spinner -> {
                metricRef.apply {
                    setValue(Metric.List(), priority)
                    child(FIREBASE_VALUE).child("a").setPriority(0)
                }
            }
            R.id.add_header -> metricRef.setValue(Metric.Header(), priority)
            else -> throw IllegalStateException("Unknown id: $id")
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
        fun newInstance(templateKey: String) =
                TemplateFragment().apply { arguments = getTabKeyBundle(templateKey) }
    }
}

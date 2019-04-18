package com.supercilex.robotscouter.feature.templates

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.observe
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.appindexing.FirebaseUserActions
import com.google.firebase.firestore.CollectionReference
import com.supercilex.robotscouter.Refreshable
import com.supercilex.robotscouter.core.data.asLiveData
import com.supercilex.robotscouter.core.data.defaultTemplateId
import com.supercilex.robotscouter.core.data.getTabId
import com.supercilex.robotscouter.core.data.getTabIdBundle
import com.supercilex.robotscouter.core.data.getTemplateViewAction
import com.supercilex.robotscouter.core.data.logFailures
import com.supercilex.robotscouter.core.data.logSelectTemplate
import com.supercilex.robotscouter.core.data.model.add
import com.supercilex.robotscouter.core.data.model.deleteMetrics
import com.supercilex.robotscouter.core.data.model.getTemplateMetricsRef
import com.supercilex.robotscouter.core.data.model.restoreMetrics
import com.supercilex.robotscouter.core.model.Metric
import com.supercilex.robotscouter.core.ui.LifecycleAwareLazy
import com.supercilex.robotscouter.core.ui.animatePopReveal
import com.supercilex.robotscouter.core.unsafeLazy
import com.supercilex.robotscouter.shared.scouting.MetricListFragment
import kotlinx.android.synthetic.main.fragment_template_metric_list.*
import org.jetbrains.anko.design.longSnackbar
import com.supercilex.robotscouter.R as RC

internal class TemplateFragment : MetricListFragment(R.layout.fragment_template_metric_list),
        Refreshable, View.OnClickListener {
    override val metricsRef: CollectionReference by unsafeLazy {
        getTemplateMetricsRef(checkNotNull(getTabId(arguments)))
    }
    override val dataId by unsafeLazy { checkNotNull(metricsRef.parent).id }

    private val itemTouchCallback by LifecycleAwareLazy {
        TemplateItemTouchCallback<Metric<*>>(requireView())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val parent = parentFragment as TemplateListFragment
        val fab = parent.fab

        noMetricsHint.animatePopReveal(true)
        holder.metrics.asLiveData().observe(viewLifecycleOwner) {
            val noMetrics = it.isEmpty()
            noMetricsHint.animatePopReveal(noMetrics)
            if (noMetrics) fab.show()
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchCallback)
        itemTouchCallback.itemTouchHelper = itemTouchHelper
        itemTouchCallback.adapter = adapter as TemplateAdapter
        itemTouchHelper.attachToRecyclerView(metricsView)

        metricsView.setRecycledViewPool(parent.recyclerPool)
        metricsView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0) fab.hide() else if (dy < 0) fab.show()
            }
        })
    }

    override fun onCreateRecyclerAdapter(savedInstanceState: Bundle?) = TemplateAdapter(
            this,
            holder.metrics,
            metricsView,
            savedInstanceState,
            itemTouchCallback
    )

    override fun refresh() {
        metricsView.smoothScrollToPosition(0)
    }

    override fun onResume() {
        super.onResume()
        logAnalytics(true)
    }

    override fun onPause() {
        super.onPause()
        logAnalytics(false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) =
            inflater.inflate(R.menu.template_options, menu)

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_set_default_template -> {
                val oldDefaultId = defaultTemplateId
                defaultTemplateId = checkNotNull(metricsRef.parent).id

                metricsView.longSnackbar(R.string.template_set_default_message, RC.string.undo) {
                    defaultTemplateId = oldDefaultId
                }
            }
            R.id.action_delete_template -> {
                metricsView.clearFocus()
                DeleteTemplateDialog.show(childFragmentManager, checkNotNull(metricsRef.parent))
            }
            R.id.action_remove_metrics -> {
                metricsView.clearFocus()
                deleteMetrics(metricsRef).addOnSuccessListener(requireActivity()) { metrics ->
                    metricsView.longSnackbar(RC.string.deleted, RC.string.undo) {
                        restoreMetrics(metrics)
                    }
                }
            }
            else -> return false
        }
        return true
    }

    override fun onClick(v: View) {
        val position = adapter.itemCount
        val metricRef = metricsRef.document()

        itemTouchCallback.addItemToScrollQueue(position)

        when (val id = v.id) {
            R.id.addHeader -> Metric.Header(position = position, ref = metricRef)
            R.id.addCheckBox -> Metric.Boolean(position = position, ref = metricRef)
            R.id.addCounter -> Metric.Number(position = position, ref = metricRef)
            R.id.addStopwatch -> Metric.Stopwatch(position = position, ref = metricRef)
            R.id.addNote -> Metric.Text(position = position, ref = metricRef)
            R.id.addSpinner -> Metric.List(position = position, ref = metricRef)
            else -> error("Unknown view id: $id")
        }.add()
    }

    private fun logAnalytics(isVisible: Boolean) {
        val pagerAdapter = (parentFragment as? TemplateListFragment ?: return).pagerAdapter
        val currentTabId = pagerAdapter.currentTabId ?: return
        val tabName = pagerAdapter.currentTab?.text.toString()

        if (isVisible) {
            logSelectTemplate(currentTabId, tabName)
            FirebaseUserActions.getInstance().start(getTemplateViewAction(currentTabId, tabName))
        } else {
            FirebaseUserActions.getInstance().end(getTemplateViewAction(currentTabId, tabName))
        }.logFailures("startOrEndTemplateAction")
    }

    companion object {
        fun newInstance(templateId: String) =
                TemplateFragment().apply { arguments = getTabIdBundle(templateId) }
    }
}

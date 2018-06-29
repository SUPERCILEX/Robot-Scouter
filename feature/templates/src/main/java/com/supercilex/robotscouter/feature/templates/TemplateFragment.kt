package com.supercilex.robotscouter.feature.templates

import android.os.Bundle
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.google.firebase.appindexing.FirebaseUserActions
import com.google.firebase.firestore.CollectionReference
import com.supercilex.robotscouter.core.data.asLiveData
import com.supercilex.robotscouter.core.data.defaultTemplateId
import com.supercilex.robotscouter.core.data.firestoreBatch
import com.supercilex.robotscouter.core.data.getTabId
import com.supercilex.robotscouter.core.data.getTabIdBundle
import com.supercilex.robotscouter.core.data.getTemplateViewAction
import com.supercilex.robotscouter.core.data.logAdd
import com.supercilex.robotscouter.core.data.logSelectTemplate
import com.supercilex.robotscouter.core.data.model.getTemplateMetricsRef
import com.supercilex.robotscouter.core.data.observeNonNull
import com.supercilex.robotscouter.core.logFailures
import com.supercilex.robotscouter.core.model.Metric
import com.supercilex.robotscouter.core.ui.RecyclerPoolHolder
import com.supercilex.robotscouter.core.ui.animatePopReveal
import com.supercilex.robotscouter.core.unsafeLazy
import com.supercilex.robotscouter.shared.scouting.MetricListFragment
import kotlinx.android.synthetic.main.fragment_template_metric_list.*
import kotlinx.coroutines.experimental.async
import org.jetbrains.anko.design.longSnackbar
import com.supercilex.robotscouter.R as RC

internal class TemplateFragment : MetricListFragment(), View.OnClickListener {
    override val metricsRef: CollectionReference by unsafeLazy {
        getTemplateMetricsRef(checkNotNull(getTabId(arguments)))
    }
    override val dataId by unsafeLazy { checkNotNull(metricsRef.parent).id }

    private val itemTouchCallback by unsafeLazy {
        TemplateItemTouchCallback<Metric<*>>(checkNotNull(view))
    }

    private var hasAddedItem: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        holder.metrics.asLiveData().observeNonNull(this) {
            noMetricsHint.animatePopReveal(it.isEmpty())
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View = View.inflate(context, R.layout.fragment_template_metric_list, null)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        noMetricsHint.animatePopReveal(true)

        val itemTouchHelper = ItemTouchHelper(itemTouchCallback)
        itemTouchCallback.itemTouchHelper = itemTouchHelper
        itemTouchCallback.adapter = adapter as TemplateAdapter
        itemTouchHelper.attachToRecyclerView(metricsView)

        metricsView.recycledViewPool = (parentFragment as RecyclerPoolHolder).recyclerPool
    }

    override fun onCreateRecyclerAdapter(savedInstanceState: Bundle?) = TemplateAdapter(
            holder.metrics,
            this,
            childFragmentManager,
            metricsView,
            savedInstanceState,
            itemTouchCallback
    )

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) =
            inflater.inflate(R.menu.template_options, menu)

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            R.id.action_set_default_template -> {
                val oldDefaultId = defaultTemplateId
                defaultTemplateId = checkNotNull(metricsRef.parent).id

                longSnackbar(metricsView, R.string.template_set_default_message, RC.string.undo) {
                    defaultTemplateId = oldDefaultId
                }
            }
            R.id.action_delete_template -> {
                metricsView.clearFocus()
                DeleteTemplateDialog.show(childFragmentManager, checkNotNull(metricsRef.parent))
            }
            R.id.action_remove_metrics -> {
                metricsView.clearFocus()
                metricsRef.get().addOnSuccessListener(requireActivity()) { metrics ->
                    async {
                        firestoreBatch {
                            for (metric in metrics) delete(metric.reference)
                        }.logFailures(metrics.map { it.reference }, metrics)
                    }.logFailures()

                    longSnackbar(metricsView, RC.string.deleted, RC.string.undo) {
                        async {
                            firestoreBatch {
                                for (metric in metrics) set(metric.reference, metric.data)
                            }.logFailures(metrics.map { it.reference }, metrics)
                        }.logFailures()
                    }
                }.logFailures(metricsRef)
            }
            else -> return false
        }
        return true
    }

    override fun onClick(v: View) {
        val position = adapter.itemCount
        val metricRef = metricsRef.document()

        hasAddedItem = true
        itemTouchCallback.addItemToScrollQueue(position)

        when (v.id) {
            R.id.addHeader -> Metric.Header(position = position, ref = metricRef)
            R.id.addCheckBox -> Metric.Boolean(position = position, ref = metricRef)
            R.id.addCounter -> Metric.Number(position = position, ref = metricRef)
            R.id.addStopwatch -> Metric.Stopwatch(position = position, ref = metricRef)
            R.id.addNote -> Metric.Text(position = position, ref = metricRef)
            R.id.addSpinner -> Metric.List(position = position, ref = metricRef)
            else -> error("Unknown view id: $id")
        }.apply {
            logAdd()
            ref.set(this).logFailures(ref, this)
        }
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        val pagerAdapter = (parentFragment as? TemplateListFragment ?: return).pagerAdapter
        val currentTabId = pagerAdapter.currentTabId ?: return
        val tabName = pagerAdapter.currentTab?.text.toString()

        if (isVisibleToUser) {
            logSelectTemplate(currentTabId, tabName)
            FirebaseUserActions.getInstance().start(getTemplateViewAction(currentTabId, tabName))
        } else {
            FirebaseUserActions.getInstance().end(getTemplateViewAction(currentTabId, tabName))
        }.logFailures()
    }

    companion object {
        fun newInstance(templateId: String) =
                TemplateFragment().apply { arguments = getTabIdBundle(templateId) }
    }
}

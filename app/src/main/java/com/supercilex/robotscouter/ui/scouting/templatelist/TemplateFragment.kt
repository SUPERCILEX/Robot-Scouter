package com.supercilex.robotscouter.ui.scouting.templatelist

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import com.github.clans.fab.FloatingActionMenu
import com.google.firebase.appindexing.FirebaseUserActions
import com.google.firebase.firestore.CollectionReference
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Metric
import com.supercilex.robotscouter.ui.scouting.MetricListFragment
import com.supercilex.robotscouter.util.data.asLiveData
import com.supercilex.robotscouter.util.data.defaultTemplateId
import com.supercilex.robotscouter.util.data.firestoreBatch
import com.supercilex.robotscouter.util.data.getTabId
import com.supercilex.robotscouter.util.data.getTabIdBundle
import com.supercilex.robotscouter.util.data.getTemplateViewAction
import com.supercilex.robotscouter.util.data.model.getTemplateMetricsRef
import com.supercilex.robotscouter.util.logAdd
import com.supercilex.robotscouter.util.logFailures
import com.supercilex.robotscouter.util.logSelectTemplate
import com.supercilex.robotscouter.util.ui.OnBackPressedListener
import com.supercilex.robotscouter.util.ui.RecyclerPoolHolder
import com.supercilex.robotscouter.util.ui.animatePopReveal
import com.supercilex.robotscouter.util.unsafeLazy
import kotlinx.coroutines.experimental.async
import kotterknife.bindView
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.support.v4.find

class TemplateFragment : MetricListFragment(), View.OnClickListener, OnBackPressedListener {
    override val metricsRef: CollectionReference by unsafeLazy {
        getTemplateMetricsRef(getTabId(arguments)!!)
    }

    private val itemTouchCallback by unsafeLazy {
        TemplateItemTouchCallback<Metric<*>>(view!!)
    }
    private val appBar: AppBarLayout by unsafeLazy {
        parentFragment!!.find<AppBarLayout>(R.id.app_bar)
    }
    private val fam: FloatingActionMenu by unsafeLazy {
        parentFragment!!.find<FloatingActionMenu>(R.id.fab_menu)
    }
    private val noContentHint: View by bindView(R.id.no_content_hint)

    private val appBarOffsetListener = object : AppBarLayout.OnOffsetChangedListener {
        var isShowing = false

        override fun onOffsetChanged(appBar: AppBarLayout, offset: Int) {
            if (offset >= -10) { // Account for small variations
                if (!isShowing) fam.showMenuButton(true)
                isShowing = true
            } else {
                isShowing = false
                // User scrolled down -> hide the FAB
                fam.hideMenuButton(true)
            }
        }
    }
    private var hasAddedItem: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        holder.metrics.asLiveData().observe(this, Observer {
            noContentHint.animatePopReveal(it!!.isEmpty())
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val itemTouchHelper = ItemTouchHelper(itemTouchCallback)
        itemTouchCallback.itemTouchHelper = itemTouchHelper
        itemTouchCallback.adapter = adapter as TemplateAdapter
        itemTouchHelper.attachToRecyclerView(recyclerView)

        recyclerView.recycledViewPool = (parentFragment as RecyclerPoolHolder).recyclerPool
        appBar.addOnOffsetChangedListener(appBarOffsetListener)

        // This lets us close the fam when the RecyclerView it touched
        recyclerView.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                fam.close(true)
                return false
            }
        })
    }

    override fun onCreateRecyclerAdapter(savedInstanceState: Bundle?) = TemplateAdapter(
            holder.metrics,
            this,
            childFragmentManager,
            recyclerView,
            savedInstanceState,
            itemTouchCallback
    )

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) =
            inflater.inflate(R.menu.template_options, menu)

    override fun onDestroyView() {
        super.onDestroyView()
        appBar.removeOnOffsetChangedListener(appBarOffsetListener)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            R.id.action_set_default_template -> {
                val oldDefaultId = defaultTemplateId
                defaultTemplateId = metricsRef.parent.id

                longSnackbar(fam, R.string.template_set_default_message, R.string.undo) {
                    defaultTemplateId = oldDefaultId
                }
            }
            R.id.action_delete_template -> {
                recyclerView.clearFocus()
                DeleteTemplateDialog.show(childFragmentManager, metricsRef.parent)
            }
            R.id.action_remove_metrics -> {
                recyclerView.clearFocus()
                metricsRef.get().addOnSuccessListener(requireActivity()) { metrics ->
                    async {
                        firestoreBatch {
                            for (metric in metrics) delete(metric.reference)
                        }.logFailures(metrics.map { it.reference }, metrics)
                    }.logFailures()

                    longSnackbar(fam, R.string.deleted, R.string.undo) {
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
        fam.close(true)
        itemTouchCallback.addItemToScrollQueue(position)

        when (v.id) {
            R.id.add_checkbox -> Metric.Boolean(position = position, ref = metricRef)
            R.id.add_counter -> Metric.Number(position = position, ref = metricRef)
            R.id.add_stopwatch -> Metric.Stopwatch(position = position, ref = metricRef)
            R.id.add_note -> Metric.Text(position = position, ref = metricRef)
            R.id.add_spinner -> Metric.List(position = position, ref = metricRef)
            R.id.add_header -> Metric.Header(position = position, ref = metricRef)
            else -> error("Unknown view id: $id")
        }.apply {
            logAdd()
            ref.set(this).logFailures(ref, this)
        }
    }

    override fun onBackPressed(): Boolean = if (fam.isOpened) {
        fam.close(true)
        true
    } else {
        false
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        val pagerAdapter = (parentFragment as? TemplateListFragment ?: return).pagerAdapter
        pagerAdapter.currentTabId!!.let {
            val tabName = pagerAdapter.currentTab?.text.toString()
            if (isVisibleToUser) {
                logSelectTemplate(it, tabName)
                FirebaseUserActions.getInstance().start(getTemplateViewAction(it, tabName))
            } else {
                FirebaseUserActions.getInstance().end(getTemplateViewAction(it, tabName))
            }.logFailures()
        }
    }

    companion object {
        fun newInstance(templateId: String) =
                TemplateFragment().apply { arguments = getTabIdBundle(templateId) }
    }
}

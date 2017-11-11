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
import com.google.firebase.firestore.CollectionReference
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Metric
import com.supercilex.robotscouter.ui.scouting.MetricListFragment
import com.supercilex.robotscouter.util.async
import com.supercilex.robotscouter.util.data.asLiveData
import com.supercilex.robotscouter.util.data.defaultTemplateId
import com.supercilex.robotscouter.util.data.delete
import com.supercilex.robotscouter.util.data.firestoreBatch
import com.supercilex.robotscouter.util.data.getTabId
import com.supercilex.robotscouter.util.data.getTabIdBundle
import com.supercilex.robotscouter.util.data.model.getTemplateMetricsRef
import com.supercilex.robotscouter.util.logFailures
import com.supercilex.robotscouter.util.ui.OnBackPressedListener
import com.supercilex.robotscouter.util.ui.RecyclerPoolHolder
import com.supercilex.robotscouter.util.ui.animatePopReveal
import com.supercilex.robotscouter.util.unsafeLazy
import kotterknife.bindView
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.support.v4.find

class TemplateFragment : MetricListFragment(), View.OnClickListener, OnBackPressedListener {
    public override val metricsRef: CollectionReference by unsafeLazy {
        getTemplateMetricsRef(getTabId(arguments)!!)
    }

    private val itemTouchCallback by unsafeLazy {
        TemplateItemTouchCallback<Metric<*>>(view!!)
    }
    private val fam: FloatingActionMenu by unsafeLazy {
        parentFragment!!.find<FloatingActionMenu>(R.id.fab_menu)
    }
    private val noContentHint: View by bindView(R.id.no_content_hint)

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
        parentFragment!!.find<AppBarLayout>(R.id.app_bar)
                .addOnOffsetChangedListener(object : AppBarLayout.OnOffsetChangedListener {
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
                })

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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            R.id.action_set_default_template -> defaultTemplateId = metricsRef.parent.id
            R.id.action_delete_template -> {
                recyclerView.clearFocus()
                DeleteTemplateDialog.show(childFragmentManager, metricsRef.parent)
            }
            R.id.action_remove_metrics -> {
                recyclerView.clearFocus()
                metricsRef.delete().addOnSuccessListener { documents ->
                    longSnackbar(fam, R.string.deleted, R.string.undo) {
                        async {
                            firestoreBatch {
                                for (document in documents) {
                                    set(document.reference, document.data)
                                }
                            }
                        }.logFailures()
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

        hasAddedItem = true
        fam.close(true)
        itemTouchCallback.addItemToScrollQueue(position)

        when (v.id) {
            R.id.add_checkbox -> metricRef.set(Metric.Boolean(position = position))
            R.id.add_counter -> metricRef.set(Metric.Number(position = position))
            R.id.add_stopwatch -> metricRef.set(Metric.Stopwatch(position = position))
            R.id.add_note -> metricRef.set(Metric.Text(position = position))
            R.id.add_spinner -> metricRef.set(Metric.List(position = position))
            R.id.add_header -> metricRef.set(Metric.Header(position = position))
            else -> throw IllegalStateException("Unknown view id: $id")
        }
    }

    override fun onBackPressed(): Boolean = if (fam.isOpened) {
        fam.close(true)
        true
    } else {
        false
    }

    companion object {
        fun newInstance(templateId: String) =
                TemplateFragment().apply { arguments = getTabIdBundle(templateId) }
    }
}

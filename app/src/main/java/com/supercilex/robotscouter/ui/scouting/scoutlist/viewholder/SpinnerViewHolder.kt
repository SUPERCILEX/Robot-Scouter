package com.supercilex.robotscouter.ui.scouting.scoutlist.viewholder

import android.support.annotation.CallSuper
import android.text.TextUtils
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Metric
import com.supercilex.robotscouter.ui.scouting.MetricViewHolderBase
import kotterknife.bindView
import java.util.ArrayList

open class SpinnerViewHolder(
        itemView: View
) : MetricViewHolderBase<Metric.List, Map<String, String>, TextView>(itemView),
        AdapterView.OnItemSelectedListener {
    protected val spinner: Spinner by bindView(R.id.spinner)
    private lateinit var ids: List<String>

    init {
        spinner.adapter = ArrayAdapter<String>(
                itemView.context,
                android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinner.onItemSelectedListener = this
    }

    public override fun bind() {
        super.bind()
        ids = ArrayList(metric.value.keys)

        updateAdapter()
        spinner.setSelection(indexOfKey(metric.selectedValueId))
    }

    protected open fun updateAdapter() {
        (spinner.adapter as ArrayAdapter<String>).apply {
            clear()
            addAll(metric.value.values)
        }
    }

    @CallSuper
    override fun onItemSelected(parent: AdapterView<*>, view: View, itemPosition: Int, id: Long) {
        metric.selectedValueId = ids[itemPosition]
    }

    protected open fun indexOfKey(key: String?): Int =
            ids.indices.firstOrNull { TextUtils.equals(key, ids[it]) } ?: 0

    override fun onNothingSelected(view: AdapterView<*>) = Unit
}

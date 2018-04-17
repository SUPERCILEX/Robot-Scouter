package com.supercilex.robotscouter.feature.templates.viewholder

import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.updateLayoutParams
import com.supercilex.robotscouter.core.data.model.updateName
import com.supercilex.robotscouter.core.data.model.updateUnit
import com.supercilex.robotscouter.core.data.nullOrFull
import com.supercilex.robotscouter.core.model.Metric
import com.supercilex.robotscouter.core.unsafeLazy
import com.supercilex.robotscouter.feature.templates.R
import com.supercilex.robotscouter.shared.scouting.viewholder.CounterViewHolder
import kotlinx.android.synthetic.main.scout_template_base_reorder.*
import kotlinx.android.synthetic.main.scout_template_counter.*
import org.jetbrains.anko.find
import java.util.Locale

internal class CounterTemplateViewHolder(itemView: View) : CounterViewHolder(itemView),
        MetricTemplateViewHolder<Metric.Number, Long> {
    override val reorderView: ImageView by unsafeLazy { reorder }
    override val nameEditor = name as EditText
    override val valueWithoutUnit: String get() = count.text.toString()

    private val count = containerView.find<TextView>(R.id.count)

    init {
        init()

        itemView as LinearLayout
        itemView.removeView(unit)
        itemView.addView(unit, itemView.childCount - 1)
        count.updateLayoutParams<LinearLayout.LayoutParams> { rightMargin = 0 }

        unit.onFocusChangeListener = this
    }

    override fun bind() {
        super.bind()
        unit.setText(metric.unit)
    }

    override fun onClick(v: View) {
        super.onClick(v)
        if (name.hasFocus()) metric.updateName(name.text.toString())
    }

    override fun setValue() {
        count.text = String.format(Locale.getDefault(), "%d", metric.value)
    }

    override fun onFocusChange(v: View, hasFocus: Boolean) {
        super.onFocusChange(v, hasFocus)
        if (!hasFocus && v === unit) {
            metric.updateUnit(unit.text.nullOrFull()?.toString())
        }
    }
}

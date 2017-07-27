package com.supercilex.robotscouter.ui.scouting.template.viewholder

import android.text.TextUtils
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.ui.scouting.scout.viewholder.CounterViewHolder

class CounterTemplateViewHolder(itemView: View) : CounterViewHolder(itemView), TemplateViewHolder {
    private val unit: EditText = itemView.findViewById(R.id.unit)

    init {
        itemView as LinearLayout
        itemView.removeView(unit)
        itemView.addView(unit, itemView.childCount - 1)
        (count.layoutParams as LinearLayout.LayoutParams).rightMargin = 0
    }

    override fun bind() {
        super.bind()
        unit.setText(metric.unit)

        name.onFocusChangeListener = this
        unit.onFocusChangeListener = this
    }

    override fun onClick(v: View) {
        super.onClick(v)
        if (name.hasFocus()) updateMetricName(name.text.toString())
    }

    override fun setValue(value: Long) {
        count.text = metric.value.toString()
    }

    override fun requestFocus() {
        name.requestFocus()
    }

    override fun onFocusChange(v: View, hasFocus: Boolean) {
        if (hasFocus) return  // Only save data when the user is done

        if (v.id == R.id.name) {
            updateMetricName(name.text.toString())
        } else if (v.id == R.id.unit) {
            var newUnit: String? = unit.text.toString()

            if (TextUtils.isEmpty(newUnit)) newUnit = null

            if (!TextUtils.equals(metric.unit, newUnit)) {
                disableAnimations()
                metric.unit = newUnit
            }
        }
    }
}

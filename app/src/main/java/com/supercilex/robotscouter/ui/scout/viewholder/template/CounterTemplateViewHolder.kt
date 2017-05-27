package com.supercilex.robotscouter.ui.scout.viewholder.template

import android.text.TextUtils
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout

import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.metrics.NumberMetric
import com.supercilex.robotscouter.ui.scout.viewholder.CounterViewHolder

class CounterTemplateViewHolder(itemView: View) : CounterViewHolder(itemView), ScoutTemplateViewHolder {
    private val mUnit: EditText = itemView.findViewById<EditText>(R.id.unit)

    init {
        itemView as LinearLayout
        itemView.removeView(mUnit)
        itemView.addView(mUnit, itemView.childCount - 1)
        (mCount.layoutParams as LinearLayout.LayoutParams).rightMargin = 0
    }

    override fun bind() {
        super.bind()
        mCount.text = metric.value.toString()
        mUnit.setText((metric as NumberMetric).unit)

        name.onFocusChangeListener = this
        mUnit.onFocusChangeListener = this
    }

    override fun onClick(v: View) {
        super.onClick(v)
        if (name.hasFocus()) updateMetricName(name.text.toString())
    }

    override fun requestFocus() {
        name.requestFocus()
    }

    override fun onFocusChange(v: View, hasFocus: Boolean) {
        if (hasFocus) return  // Only save data when the user is done

        if (v.id == R.id.name) {
            updateMetricName(name.text.toString())
        } else if (v.id == R.id.unit) {
            val numberMetric = metric as NumberMetric
            var newUnit: String? = mUnit.text.toString()

            if (TextUtils.isEmpty(newUnit)) newUnit = null

            if (!TextUtils.equals(numberMetric.unit, newUnit)) {
                disableAnimations()
                numberMetric.updateUnit(newUnit)
            }
        }
    }
}

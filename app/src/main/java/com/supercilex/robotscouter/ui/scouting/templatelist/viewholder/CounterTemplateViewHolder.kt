package com.supercilex.robotscouter.ui.scouting.templatelist.viewholder

import android.text.TextUtils
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.ui.scouting.scoutlist.viewholder.CounterViewHolder
import com.supercilex.robotscouter.util.unsafeLazy
import kotterknife.bindView

class CounterTemplateViewHolder(itemView: View) : CounterViewHolder(itemView), TemplateViewHolder {
    override val reorder: View by bindView(R.id.reorder)
    override val nameEditor: EditText by unsafeLazy { name as EditText }
    private val unit: EditText by bindView(R.id.unit)

    init {
        itemView as LinearLayout
        itemView.removeView(unit)
        itemView.addView(unit, itemView.childCount - 1)
        (count.layoutParams as LinearLayout.LayoutParams).rightMargin = 0
    }

    override fun bind() {
        super<CounterViewHolder>.bind()
        super<TemplateViewHolder>.bind()
        unit.setText(metric.unit)

        unit.onFocusChangeListener = this
    }

    override fun onClick(v: View) {
        super.onClick(v)
        if (name.hasFocus()) metric.name = name.text.toString()
    }

    override fun setValue() {
        count.text = metric.value.toString()
    }

    override fun onFocusChange(v: View, hasFocus: Boolean) {
        if (hasFocus) return // Only save data when the user is done

        if (v.id == R.id.name) {
            metric.name = name.text.toString()
        } else if (v.id == R.id.unit) {
            var newUnit: String? = unit.text.toString()

            if (TextUtils.isEmpty(newUnit)) newUnit = null

            if (!TextUtils.equals(metric.unit, newUnit)) {
                metric.unit = newUnit
            }
        }
    }
}

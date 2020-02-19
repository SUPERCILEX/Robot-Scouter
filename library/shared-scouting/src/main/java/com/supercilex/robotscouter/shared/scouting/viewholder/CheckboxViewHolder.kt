package com.supercilex.robotscouter.shared.scouting.viewholder

import android.view.View
import com.supercilex.robotscouter.core.data.model.update
import com.supercilex.robotscouter.core.model.Metric
import com.supercilex.robotscouter.shared.scouting.MetricViewHolderBase
import com.supercilex.robotscouter.shared.scouting.R
import com.supercilex.robotscouter.shared.scouting.databinding.ScoutBaseCheckboxBinding

open class CheckboxViewHolder(
        itemView: View
) : MetricViewHolderBase<Metric.Boolean, Boolean>(itemView), View.OnClickListener {
    private val binding = ScoutBaseCheckboxBinding.bind(itemView)

    init {
        binding.checkbox.setOnClickListener(this)
        binding.root.findViewById<View>(R.id.name).setOnClickListener(this)
    }

    public override fun bind() {
        super.bind()
        binding.checkbox.isChecked = metric.value
        binding.checkbox.jumpDrawablesToCurrentState() // Skip animation on first load
    }

    override fun onClick(v: View) {
        if (v.id == R.id.checkbox) metric.update(binding.checkbox.isChecked)
        if (v.id == R.id.name) binding.checkbox.performClick()
    }
}

package com.supercilex.robotscouter.ui.scout.viewholder

import android.support.annotation.CallSuper
import android.support.v4.app.FragmentManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import android.text.TextUtils
import android.view.View
import android.widget.TextView
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.metrics.ScoutMetric

abstract class ScoutViewHolderBase<TMetric, out VView : TextView>(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
    @Suppress("UNCHECKED_CAST")
    protected val mName: VView = itemView.findViewById(R.id.name) as VView
    protected lateinit var mMetric: ScoutMetric<TMetric>
    protected lateinit var mManager: FragmentManager
    private lateinit var mAnimator: SimpleItemAnimator

    fun bind(metric: ScoutMetric<TMetric>, manager: FragmentManager, animator: SimpleItemAnimator) {
        mMetric = metric
        mManager = manager
        mAnimator = animator

        bind()
    }

    @CallSuper
    protected open fun bind() {
        mName.text = mMetric.name
    }

    protected fun updateMetricName(name: String) {
        if (!TextUtils.equals(mMetric.name, name)) {
            disableAnimations()
            mMetric.updateName(name)
        }
    }

    protected fun updateMetricValue(value: TMetric) {
        if (value != mMetric.value) {
            disableAnimations()
            mMetric.updateValue(value)
        }
    }

    protected fun disableAnimations() {
        mAnimator.supportsChangeAnimations = false
    }

    override fun toString() = mMetric.toString()
}

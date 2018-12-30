package com.supercilex.robotscouter.shared.scouting.viewholder

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.isVisible
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.supercilex.robotscouter.core.data.model.update
import com.supercilex.robotscouter.core.model.Metric
import com.supercilex.robotscouter.core.ui.setOnLongClickListenerCompat
import com.supercilex.robotscouter.core.ui.shortAnimationDuration
import com.supercilex.robotscouter.shared.scouting.CounterValueDialog
import com.supercilex.robotscouter.shared.scouting.MetricViewHolderBase
import com.supercilex.robotscouter.shared.scouting.R
import kotlinx.android.synthetic.main.scout_base_counter.*
import java.text.NumberFormat
import java.util.Locale

open class CounterViewHolder(
        itemView: View
) : MetricViewHolderBase<Metric.Number, Long>(itemView),
        View.OnClickListener, View.OnLongClickListener {
    private var count = count1
    private var tmpCount = count2

    private var countAnimation: Animator? = null

    init {
        increment.setOnClickListener(this)
        decrement.setOnClickListener(this)
        countContainer.setOnLongClickListenerCompat(this)
    }

    public override fun bind() {
        super.bind()
        update(false)
    }

    @CallSuper
    override fun onClick(v: View) {
        val id = v.id
        var value = metric.value

        val up = if (id == R.id.increment) {
            metric.update(++value)
            true
        } else if (id == R.id.decrement) {
            metric.update(--value)
            false
        } else {
            return
        }

        update(true, up)
    }

    private fun update(animate: Boolean, up: Boolean = false) {
        decrement.isEnabled = metric.value > 0 // No negative values

        if (!animate || countAnimation != null) {
            bindValue(count)
            return
        }

        val transition = {
            TransitionManager.beginDelayedTransition(
                    itemView as ViewGroup,
                    AutoTransition()
                            .setDuration(shortAnimationDuration)
                            .excludeTarget(R.id.count1, true)
                            .excludeTarget(R.id.count2, true)
            )
        }

        val out = ValueAnimator.ofInt(count.height, 0).apply {
            val y = count.top.toFloat()
            addUpdateListener {
                tmpCount.height = it.animatedValue as Int
                if (up) {
                    // Neat trick to translate the view: since it's shrinking, its top is increasing
                    // which turns into a smaller y.
                    tmpCount.y = y
                } else {
                    tmpCount.translationY = y * it.animatedFraction
                }
            }
            doOnStart { tmpCount.gravity = if (up) Gravity.BOTTOM else Gravity.TOP }
        }
        val `in` = ValueAnimator.ofInt(0, count.height).apply {
            val y = count.top.toFloat()
            addUpdateListener {
                count.height = it.animatedValue as Int
                if (up) {
                    count.translationY = 2 * y * (1 - it.animatedFraction)
                } else {
                    count.y = y * it.animatedFraction
                }
            }

            doOnStart {
                if (up) transition()
                count.apply {
                    gravity = if (up) Gravity.TOP else Gravity.BOTTOM
                    isVisible = true
                }
            }
            doOnEnd {
                if (!up) transition()
                tmpCount.isVisible = false
            }
        }

        countAnimation = AnimatorSet().apply {
            playTogether(out, `in`)
            doOnEnd { countAnimation = null }

            // Update the incoming view
            bindValue(tmpCount)

            val count = count
            this@CounterViewHolder.count = tmpCount
            tmpCount = count

            start()
        }
    }

    protected open fun bindValue(count: TextView) {
        val value = countFormat.format(metric.value)
        val unit: String? = metric.unit
        count.text = if (unit.isNullOrBlank()) value else value + unit
    }

    override fun onLongClick(v: View): Boolean {
        CounterValueDialog.show(fragmentManager, metric.ref, metric.value.toString())
        return true
    }

    protected companion object {
        val countFormat: NumberFormat = NumberFormat.getInstance(Locale.US)
    }
}

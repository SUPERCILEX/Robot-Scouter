package com.supercilex.robotscouter.util.ui.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.animation.LinearInterpolator
import androidx.core.animation.doOnEnd
import com.supercilex.robotscouter.util.ui.ContentLoader
import com.supercilex.robotscouter.util.ui.ContentLoaderHelper
import com.supercilex.robotscouter.util.ui.animatePopReveal

class ContentLoadingHint : SupportVectorDrawablesTextView, ContentLoader,
        ValueAnimator.AnimatorUpdateListener {
    override val helper = ContentLoaderHelper(this, { reveal() }, { dismiss() })

    private var animator: ValueAnimator? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr)

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        helper.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        helper.onDetachedFromWindow()
        animator?.cancel()
        animator = null
    }

    private fun reveal() {
        animatePopReveal(true, object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                startIdleAnimation()
            }
        })
    }

    private fun dismiss() {
        animator?.apply {
            end()
            doOnEnd { animatePopReveal(false) }
        }
        animator = null
    }

    private fun startIdleAnimation() {
        animator = ValueAnimator.ofFloat(1f, 1.15f, 0.85f, 1f).apply {
            interpolator = LinearInterpolator()
            duration = DURATION
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener(this@ContentLoadingHint)
            start()
        }
    }

    override fun onAnimationUpdate(animation: ValueAnimator) {
        (animation.animatedValue as Float).let {
            scaleY = it
            scaleX = it
        }
    }

    private companion object {
        const val DURATION = 2500L
    }
}

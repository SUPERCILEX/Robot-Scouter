package com.supercilex.robotscouter.core.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ArgbEvaluator
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.os.Build
import android.view.View
import android.view.ViewAnimationUtils
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import com.supercilex.robotscouter.core.RobotScouter

val shortAnimationDuration: Long by lazy {
    RobotScouter.resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
}

fun animateColorChange(
        @ColorRes from: Int,
        @ColorRes to: Int,
        listener: ValueAnimator.AnimatorUpdateListener
) {
    ValueAnimator.ofObject(
            ArgbEvaluator(),
            ContextCompat.getColor(RobotScouter, from),
            ContextCompat.getColor(RobotScouter, to)).apply {
        addUpdateListener(listener)
        start()
    }
}

fun View.animateCircularReveal(
        visible: Boolean,
        centerX: Int,
        centerY: Int,
        radius: Float
): Animator? = getRevealAnimation(visible) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
        isVisible = visible
        return@getRevealAnimation null
    }

    val anim: Animator = ViewAnimationUtils.createCircularReveal(
            this,
            centerX,
            centerY,
            if (visible) 0f else radius,
            if (visible) radius else 0f
    )

    anim.addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationStart(animation: Animator?) {
            if (visible) isVisible = true
        }

        override fun onAnimationEnd(animation: Animator) {
            if (!visible) isVisible = false
        }
    })

    anim
}

fun View.animatePopReveal(
        visible: Boolean,
        listener: Animator.AnimatorListener? = null
) = getRevealAnimation(visible) {
    if (visible) {
        alpha = 0f
        scaleY = 0f
        scaleX = 0f
        isVisible = true
    }

    animate().cancel()
    animate()
            .scaleX(if (visible) 1f else 0f)
            .scaleY(if (visible) 1f else 0f)
            .alpha(if (visible) 1f else 0f)
            .setDuration(shortAnimationDuration)
            // TODO sadly, LookupTableInterpolator is package private in Java which makes Kotlin
            // throw an IllegalAccessError. See https://youtrack.jetbrains.com/issue/KT-15315.
            .setInterpolator(@Suppress("USELESS_CAST") if (visible) {
                LinearOutSlowInInterpolator() as Any
            } else {
                FastOutLinearInInterpolator() as Any
            } as TimeInterpolator)
            .setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                    listener?.onAnimationStart(animation)
                }

                override fun onAnimationCancel(animation: Animator) {
                    isGone = visible

                    listener?.onAnimationCancel(animation)
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (!visible) isVisible = false
                    // Reset state
                    alpha = 1f
                    scaleY = 1f
                    scaleX = 1f

                    listener?.onAnimationEnd(animation)
                }

                override fun onAnimationRepeat(animation: Animator) {
                    listener?.onAnimationRepeat(animation)
                }
            })
}

private inline fun <T> View.getRevealAnimation(visible: Boolean, animator: () -> T?): T? {
    return if (visible && isVisible || !visible && !isVisible) {
        null
    } else if (!ViewCompat.isAttachedToWindow(this)) {
        isVisible = visible
        null
    } else {
        animator()
    }
}

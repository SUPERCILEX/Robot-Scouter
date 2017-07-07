package com.supercilex.robotscouter.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.support.annotation.ColorRes
import android.support.design.widget.TextInputEditText
import android.support.design.widget.TextInputLayout
import android.support.text.emoji.widget.EmojiAppCompatEditText
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.View
import android.view.ViewAnimationUtils
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection

fun isInTabletMode(context: Context): Boolean {
    val config: Configuration = context.resources.configuration
    val size: Int = config.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
    return size == Configuration.SCREENLAYOUT_SIZE_LARGE && config.orientation == Configuration.ORIENTATION_LANDSCAPE
            || size > Configuration.SCREENLAYOUT_SIZE_LARGE
}

fun animateColorChange(context: Context,
                       @ColorRes from: Int,
                       @ColorRes to: Int,
                       listener: ValueAnimator.AnimatorUpdateListener) {
    ValueAnimator.ofObject(
            ArgbEvaluator(),
            ContextCompat.getColor(context, from),
            ContextCompat.getColor(context, to)).apply {
        addUpdateListener(listener)
        start()
    }
}

fun animateCircularReveal(view: View, visible: Boolean) {
    val centerX: Int = view.width / 2
    val centerY: Int = view.height / 2
    val animator: Animator? = animateCircularReveal(
            view,
            visible,
            centerX,
            centerY,
            Math.hypot(centerX.toDouble(), centerY.toDouble()).toFloat())
    animator?.start()
}

fun animateCircularReveal(view: View,
                          visible: Boolean,
                          centerX: Int,
                          centerY: Int,
                          radius: Float): Animator? {
    if (visible && view.visibility == View.VISIBLE || !visible && view.visibility == View.GONE) {
        return null
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        if (!view.isAttachedToWindow) {
            view.visibility = if (visible) View.VISIBLE else View.GONE
            return null
        }

        val anim: Animator = ViewAnimationUtils.createCircularReveal(
                view,
                centerX,
                centerY,
                if (visible) 0f else radius,
                if (visible) radius else 0f)

        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (!visible) view.visibility = View.GONE
            }
        })
        if (visible) view.visibility = View.VISIBLE

        return anim
    } else {
        view.visibility = if (visible) View.VISIBLE else View.GONE
        return null
    }
}

/**
 * Wrapper emoji compat class to support showing hint when phone is in landscape mode i.e. when IME
 * is in 'extract' mode.
 */
class EmojiCompatTextInputEditText : EmojiAppCompatEditText {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr)

    /** Copied from [TextInputEditText] */
    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val ic = super.onCreateInputConnection(outAttrs)
        if (ic != null && outAttrs.hintText == null) {
            // If we don't have a hint and our parent is a TextInputLayout, use it's hint for the
            // EditorInfo. This allows us to display a hint in 'extract mode'.
            var parent = parent
            while (parent is View) {
                if (parent is TextInputLayout) {
                    outAttrs.hintText = parent.hint
                    break
                }
                parent = parent.getParent()
            }
        }
        return ic
    }
}

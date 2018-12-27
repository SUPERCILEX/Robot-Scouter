package com.supercilex.robotscouter.core.ui.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Animatable
import android.graphics.drawable.Animatable2
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.ViewCompat
import androidx.core.view.postDelayed
import androidx.core.widget.TextViewCompat
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import com.supercilex.robotscouter.core.ui.ContentLoader
import com.supercilex.robotscouter.core.ui.ContentLoaderHelper
import com.supercilex.robotscouter.core.ui.animatePopReveal
import com.supercilex.robotscouter.core.ui.mediumAnimationDuration
import com.supercilex.robotscouter.core.unsafeLazy

class ContentLoadingHint : AppCompatTextView, ContentLoader {
    override val helper = ContentLoaderHelper(this, ::toggle)

    private val animatable by unsafeLazy {
        TextViewCompat.getCompoundDrawablesRelative(this)
                .filterNotNull()
                .single() as Animatable
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr)

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        helper.onAttachedToWindow()

        animatable.registerOnEndCallback {
            postDelayed(mediumAnimationDuration) {
                if (ViewCompat.isAttachedToWindow(this@ContentLoadingHint)) animatable.start()
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        helper.onDetachedFromWindow()

        animatable.clearAnimationCallbacks()
        animatable.stop()
    }

    private fun toggle(visible: Boolean) {
        animatePopReveal(visible, object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (visible) animatable.start() else animatable.stop()
            }
        })
    }

    @SuppressLint("NewApi")
    private inline fun Animatable.registerOnEndCallback(crossinline onEnd: () -> Unit) {
        if (this is Animatable2Compat) {
            registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
                override fun onAnimationEnd(drawable: Drawable?) {
                    onEnd()
                }
            })
        } else {
            this as Animatable2
            registerAnimationCallback(object : Animatable2.AnimationCallback() {
                override fun onAnimationEnd(drawable: Drawable?) {
                    onEnd()
                }
            })
        }
    }

    @SuppressLint("NewApi")
    private fun Animatable.clearAnimationCallbacks() {
        if (this is Animatable2Compat) {
            clearAnimationCallbacks()
        } else {
            this as Animatable2
            clearAnimationCallbacks()
        }
    }
}

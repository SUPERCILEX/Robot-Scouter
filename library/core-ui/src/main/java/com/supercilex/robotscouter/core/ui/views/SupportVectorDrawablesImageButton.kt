package com.supercilex.robotscouter.core.ui.views

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.withStyledAttributes
import com.supercilex.robotscouter.core.ui.R
import com.supercilex.robotscouter.core.ui.getDrawableCompat
import com.supercilex.robotscouter.core.ui.getIconThemedContext

/** Supports custom icon styling. */
open class SupportVectorDrawablesImageButton : AppCompatImageButton {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        applyDrawable(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr) {
        applyDrawable(attrs)
    }

    private fun applyDrawable(set: AttributeSet) {
        context.withStyledAttributes(set, R.styleable.Icon) {
            setImageDrawable(getIconThemedContext(context).getDrawableCompat(
                    getResourceId(R.styleable.Icon_iconDrawable, -1)
            ))
        }
    }
}

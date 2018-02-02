package com.supercilex.robotscouter.util.ui.views

import android.content.Context
import android.support.v7.widget.AppCompatImageButton
import android.util.AttributeSet
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.util.ui.getDrawableCompat
import com.supercilex.robotscouter.util.ui.getThemedContext
import com.supercilex.robotscouter.util.ui.obtainStyledAttributes

/** @see SupportVectorDrawablesTextView */
class SupportVectorDrawablesImageButton : AppCompatImageButton {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        applyDrawable(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr) {
        applyDrawable(attrs)
    }

    private fun applyDrawable(set: AttributeSet) {
        context.obtainStyledAttributes(set, R.styleable.Icon) {
            setImageDrawable(getThemedContext(context).getDrawableCompat(
                    getResourceId(R.styleable.Icon_iconDrawable, -1)
            ))
        }
    }
}

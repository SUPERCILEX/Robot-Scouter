package com.supercilex.robotscouter.util.ui.views

import android.content.Context
import android.support.v7.widget.AppCompatButton
import android.util.AttributeSet
import com.supercilex.robotscouter.util.ui.initSupportVectorDrawablesAttrs

/** @see SupportVectorDrawablesTextView */
class SupportVectorDrawablesButton : AppCompatButton {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initSupportVectorDrawablesAttrs(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr) {
        initSupportVectorDrawablesAttrs(attrs)
    }
}

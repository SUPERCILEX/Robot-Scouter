package com.supercilex.robotscouter.core.ui.views

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.widget.LinearLayout
import com.supercilex.robotscouter.core.ui.CardMetric
import com.supercilex.robotscouter.core.ui.CardMetricHelper

class CardMetricLinearLayout : LinearLayout, CardMetric {
    override val helper = CardMetricHelper(this)

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr)

    init {
        helper.init()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super<LinearLayout>.onLayout(changed, left, top, right, bottom)
        super<CardMetric>.onLayout(changed, left, top, right, bottom)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) =
            super<CardMetric>.onSizeChanged(w, h, oldw, oldh)

    override fun onDraw(canvas: Canvas) {
        super<LinearLayout>.onDraw(canvas)
        super<CardMetric>.onDraw(canvas)
    }
}

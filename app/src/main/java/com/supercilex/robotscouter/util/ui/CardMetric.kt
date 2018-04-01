package com.supercilex.robotscouter.util.ui

import android.annotation.TargetApi
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Paint
import android.os.Build
import android.support.v4.content.ContextCompat
import android.view.View
import android.view.ViewOutlineProvider
import com.supercilex.robotscouter.R
import android.graphics.RectF as AndroidRectF

interface CardMetric {
    val helper: CardMetricHelper

    var isFirstItem
        get() = helper.isFirstItem
        set(value) {
            helper.isFirstItem = value
        }
    var isLastItem
        get() = helper.isLastItem
        set(value) {
            helper.isLastItem = value
        }

    fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) = helper.onLayout()

    fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) = helper.onSizeChanged()

    fun onDraw(canvas: Canvas) = helper.onDraw(canvas)
}

class CardMetricHelper(private val view: View) {
    var isFirstItem = false
    var isLastItem = false

    private val backgroundColors =
            ContextCompat.getColorStateList(view.context, R.color.list_item)!!
    private val background = Paint(Paint.ANTI_ALIAS_FLAG)
    private val divider = Paint().apply {
        color = ContextCompat.getColor(view.context, R.color.list_divider)
    }

    // Divide by 2 since the view only draws half the divider
    private val dividerHeight = view.resources.getDimension(R.dimen.divider_height) / 2

    private val cornerRadius = view.resources.getDimension(R.dimen.corner_radius)
    private val cornerDiameter = 2 * cornerRadius

    private val rectF = AndroidRectF()

    private val provider by lazy {
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, cornerRadius)
            }
        }
    }

    init {
        require(Color.alpha(backgroundColors.defaultColor) == 255)
    }

    fun init() {
        view.setWillNotDraw(false)
    }

    fun onLayout() {
        background.color =
                backgroundColors.getColorForState(view.drawableState, backgroundColors.defaultColor)
    }

    fun onSizeChanged() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.outlineProvider = provider
        }
    }

    fun onDraw(canvas: Canvas) {
        val width = view.width.toFloat()
        val height = view.height.toFloat()

        val backgroundColor = background.color
        if (Color.alpha(backgroundColor) != 255) {
            background.color = backgroundColors.defaultColor
            drawBackground(canvas, width, height)
            background.color = backgroundColor
        }
        drawBackground(canvas, width, height)

        // Draw the divider
        if (!isFirstItem) canvas.drawRect(0f, 0f, width, dividerHeight, divider)
        if (!isLastItem) canvas.drawRect(0f, height - dividerHeight, width, height, divider)
    }

    private fun drawBackground(canvas: Canvas, width: Float, height: Float) {
        // Draw the corners
        if (isFirstItem) {
            canvas.drawArc(
                    RectF(0f, 0f, cornerDiameter, cornerDiameter),
                    -180f,
                    CORNER_SWEEP_ANGLE,
                    true,
                    background
            )
            canvas.drawArc(
                    RectF(width - cornerDiameter, 0f, width, cornerDiameter),
                    -90f,
                    CORNER_SWEEP_ANGLE,
                    true,
                    background
            )
        } else {
            canvas.drawRect(0f, 0f, cornerRadius, cornerRadius, background)
            canvas.drawRect(width - cornerRadius, 0f, width, cornerRadius, background)
        }
        if (isLastItem) {
            canvas.drawArc(
                    RectF(0f, height - cornerDiameter, cornerDiameter, height),
                    90f,
                    CORNER_SWEEP_ANGLE,
                    true,
                    background
            )
            canvas.drawArc(
                    RectF(width - cornerDiameter, height - cornerDiameter, width, height),
                    0f,
                    CORNER_SWEEP_ANGLE,
                    true,
                    background
            )
        } else {
            canvas.drawRect(0f, height - cornerRadius, cornerRadius, height, background)
            canvas.drawRect(
                    width - cornerRadius,
                    height - cornerRadius,
                    width,
                    height,
                    background
            )
        }

        // Draw the main background
        canvas.drawRect(0f, cornerRadius, width, height - cornerRadius, background)
        canvas.drawRect(cornerRadius, 0f, width - cornerRadius, cornerRadius, background)
        canvas.drawRect(
                cornerRadius,
                height - cornerRadius,
                width - cornerRadius,
                height,
                background
        )
    }

    @Suppress("FunctionName") // Fake class initializer
    private fun RectF(left: Float, top: Float, right: Float, bottom: Float) =
            rectF.apply { set(left, top, right, bottom) }

    private companion object {
        const val CORNER_SWEEP_ANGLE = 90f
    }
}

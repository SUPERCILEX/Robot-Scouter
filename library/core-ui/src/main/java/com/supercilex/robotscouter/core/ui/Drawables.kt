package com.supercilex.robotscouter.core.ui

import android.content.Context
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.withStyledAttributes
import androidx.core.widget.TextViewCompat
import com.supercilex.robotscouter.core.CrashLogger
import com.supercilex.robotscouter.core.LateinitVal
import org.xmlpull.v1.XmlPullParser

private const val SELECTOR_ATTR_NAME = "selector"
private const val ITEM_ATTR_NAME = "item"
private const val STATE_ATTR_NAME = "state"
private const val DRAWABLE_ATTR_NAME = "drawable"

fun Context.getDrawableCompat(@DrawableRes resId: Int): Drawable? {
    val parser = resources.getXml(resId)
    try {
        var type = parser.next()
        while (type != XmlPullParser.START_TAG && type != XmlPullParser.END_DOCUMENT) {
            type = parser.next()
        }

        if (type != XmlPullParser.START_TAG || parser.name != SELECTOR_ATTR_NAME) {
            return AppCompatResources.getDrawable(this, resId)
        }

        val states = StateListDrawable()

        type = parser.next()
        while (type != XmlPullParser.END_DOCUMENT) {
            if (type == XmlPullParser.START_TAG && parser.name == ITEM_ATTR_NAME) {
                var stateType = 0
                var drawableId: Int by LateinitVal()
                for (i in 0 until parser.attributeCount) {
                    if (parser.getAttributeName(i).startsWith(STATE_ATTR_NAME)) {
                        stateType = parser.getAttributeNameResource(i)
                    } else if (parser.getAttributeName(i) == DRAWABLE_ATTR_NAME) {
                        drawableId = parser.getAttributeResourceValue(i, -1)
                    }
                }
                states.addState(
                        intArrayOf(stateType), AppCompatResources.getDrawable(this, drawableId))
            }
            type = parser.next()
        }

        return states
    } catch (e: Exception) {
        CrashLogger.onFailure(e)
        return null
    } finally {
        parser.close()
    }
}

internal fun TextView.initSupportVectorDrawablesAttrs(attrs: AttributeSet?) {
    if (attrs == null) return

    context.withStyledAttributes(attrs, R.styleable.SupportVectorDrawables) {
        val compute: Int.() -> Drawable? = compute@{
            if (this == -1) {
                null
            } else {
                var result: Drawable? = null
                context.withStyledAttributes(attrs, R.styleable.Icon) {
                    result = getIconThemedContext(context).getDrawableCompat(this@compute)
                }
                result
            }
        }
        val drawableStart = getResourceId(
                R.styleable.SupportVectorDrawables_drawableStartCompat, -1).compute()
        val drawableEnd = getResourceId(
                R.styleable.SupportVectorDrawables_drawableEndCompat, -1).compute()
        val drawableBottom = getResourceId(
                R.styleable.SupportVectorDrawables_drawableBottomCompat, -1).compute()
        val drawableTop = getResourceId(
                R.styleable.SupportVectorDrawables_drawableTopCompat, -1).compute()

        TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(
                this@initSupportVectorDrawablesAttrs,
                drawableStart,
                drawableTop,
                drawableEnd,
                drawableBottom
        )
    }
}

internal fun TypedArray.getIconThemedContext(context: Context) = ContextThemeWrapper(
        context,
        getResourceId(R.styleable.Icon_iconStyle, R.style.RobotScouter)
)

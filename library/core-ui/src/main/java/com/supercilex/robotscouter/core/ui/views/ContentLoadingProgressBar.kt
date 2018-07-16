package com.supercilex.robotscouter.core.ui.views

import android.content.Context
import android.util.AttributeSet
import android.widget.ProgressBar
import androidx.core.view.isVisible
import com.supercilex.robotscouter.core.ui.ContentLoader
import com.supercilex.robotscouter.core.ui.ContentLoaderHelper

/**
 * ContentLoadingProgressBar implements a ProgressBar that waits a minimum time to be dismissed
 * before showing. Once visible, the progress bar will be visible for a minimum amount of time to
 * avoid "flashes" in the UI when an event could take a largely variable time to complete
 * (from none, to a user perceivable amount).
 */
class ContentLoadingProgressBar : ProgressBar, ContentLoader {
    override val helper = ContentLoaderHelper(this) { isVisible = it }

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
    }
}

package com.supercilex.robotscouter.core.ui

import android.animation.ValueAnimator
import android.annotation.TargetApi
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.RecyclerView
import com.supercilex.robotscouter.common.isSingleton
import org.jetbrains.anko.find

abstract class AllChangesSelectionObserver<T> : SelectionTracker.SelectionObserver<T>() {
    override fun onSelectionRefresh() = onSelectionChanged()

    override fun onSelectionRestored() = onSelectionChanged()
}

abstract class ItemDetailsLookupBase<T, V : RecyclerView.ViewHolder, D : ItemDetailsLookup.ItemDetails<T>>(
        private val recyclerView: RecyclerView,
        private val details: (V) -> D
) : ItemDetailsLookup<T>() {
    final override fun getItemDetails(
            e: MotionEvent
    ) = recyclerView.findChildViewUnder(e.x, e.y)?.let {
        @Suppress("UNCHECKED_CAST") // It's the consumer's job to know the type
        recyclerView.getChildViewHolder(it) as? V
    }?.let(details)
}

abstract class ItemDetailsBase<T, V : RecyclerView.ViewHolder>(
        private val holder: V
) : ItemDetailsLookup.ItemDetails<T>() {
    override fun getPosition() = holder.adapterPosition
}

abstract class MenuHelperBase<T>(
        private val activity: AppCompatActivity,
        private val tracker: SelectionTracker<T>
) : AllChangesSelectionObserver<T>(), View.OnClickListener {
    private val toolbar = activity.find<Toolbar>(R.id.toolbar)

    private var prevSelection = emptyList<T>()

    init {
        toolbar.setNavigationOnClickListener(this)

        // Initialize ColorDrawables so no-ops can be performed
        toolbar.setBackgroundColor(ContextCompat.getColor(activity, R.color.colorPrimary))
    }

    final override fun onSelectionChanged() {
        val selection = tracker.selection.toList()

        if (prevSelection == selection) return
        prevSelection = selection

        onNormalMenuChanged(selection.isEmpty())
        onSingleSelectMenuChanged(selection.isSingleton)
        onMultiSelectMenuChanged(!selection.isEmpty())

        updateToolbarTitle()
    }

    final override fun onClick(v: View) {
        val hasSelection = tracker.hasSelection()
        if (hasSelection) tracker.clearSelection()
        handleNavigationClick(hasSelection)
    }

    protected open fun handleNavigationClick(hasSelection: Boolean) = Unit

    fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        createMenu(menu, inflater)

        if (tracker.hasSelection()) {
            onNormalMenuChanged(false)
            onSingleSelectMenuChanged(tracker.selection.isSingleton)
            onMultiSelectMenuChanged(true)

            updateToolbarTitle()
        }
    }

    protected abstract fun createMenu(menu: Menu, inflater: MenuInflater)

    fun onOptionsItemSelected(item: MenuItem) = handleItemSelected(item)

    protected open fun handleItemSelected(item: MenuItem) = false

    private fun onNormalMenuChanged(visible: Boolean) {
        // Post twice: 1. for speed, 2. so the items update when restoring from a config change
        val updateItems = { updateNormalMenu(visible) }
        updateItems()
        toolbar.post(updateItems)

        updateToolbarColor(visible)
        updateNavigationIcon(visible)
    }

    protected open fun updateNormalMenu(visible: Boolean) = Unit

    protected abstract fun updateNavigationIcon(default: Boolean)

    private fun onSingleSelectMenuChanged(visible: Boolean) = updateSingleSelectMenu(visible)

    protected open fun updateSingleSelectMenu(visible: Boolean) = Unit

    private fun onMultiSelectMenuChanged(visible: Boolean) = updateMultiSelectMenu(visible)

    protected open fun updateMultiSelectMenu(visible: Boolean) = Unit

    private fun updateToolbarTitle() {
        val selection = tracker.selection
        checkNotNull(activity.supportActionBar).title = if (selection.isEmpty) {
            activity.title
        } else {
            selection.size().toString()
        }
    }

    private fun updateToolbarColor(normal: Boolean) {
        @ColorRes val oldColorPrimary =
                if (normal) R.color.selected_toolbar else R.color.colorPrimary
        @ColorRes val newColorPrimary =
                if (normal) R.color.colorPrimary else R.color.selected_toolbar

        if (toolbar.background.shouldUpdate(newColorPrimary)) {
            animateColorChange(
                    oldColorPrimary, newColorPrimary, ValueAnimator.AnimatorUpdateListener {
                toolbar.setBackgroundColor(it.animatedValue as Int)
            })
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            @ColorRes val oldColorPrimaryDark =
                    if (normal) R.color.selected_status_bar else R.color.colorPrimaryDark
            @ColorRes val newColorPrimaryDark =
                    if (normal) R.color.colorPrimaryDark else R.color.selected_status_bar

            if (shouldUpdateStatusBarColor(newColorPrimaryDark)) {
                animateColorChange(
                        oldColorPrimaryDark,
                        newColorPrimaryDark,
                        ValueAnimator.AnimatorUpdateListener {
                            updateStatusBarColor(it.animatedValue as Int)
                        }
                )
            }
        }
    }

    internal fun Drawable?.shouldUpdate(@ColorRes new: Int) =
            this !is ColorDrawable || color != ContextCompat.getColor(activity, new)

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    internal abstract fun shouldUpdateStatusBarColor(@ColorRes new: Int): Boolean

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    internal abstract fun updateStatusBarColor(@ColorInt value: Int)
}

abstract class ToolbarMenuHelperBase<T>(
        private val activity: AppCompatActivity,
        tracker: SelectionTracker<T>
) : MenuHelperBase<T>(activity, tracker) {
    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.window.statusBarColor =
                    ContextCompat.getColor(activity, R.color.colorPrimaryDark)
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    override fun shouldUpdateStatusBarColor(new: Int): Boolean =
            activity.window.statusBarColor != ContextCompat.getColor(activity, new)

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    override fun updateStatusBarColor(value: Int) {
        activity.window.statusBarColor = value
    }
}

abstract class DrawerMenuHelperBase<T>(
        activity: AppCompatActivity,
        tracker: SelectionTracker<T>
) : MenuHelperBase<T>(activity, tracker) {
    private val drawer = activity.find<DrawerLayout>(R.id.drawerLayout)

    init {
        drawer.setStatusBarBackgroundColor(
                ContextCompat.getColor(activity, R.color.colorPrimaryDark))
    }

    override fun shouldUpdateStatusBarColor(new: Int) =
            drawer.statusBarBackgroundDrawable.shouldUpdate(new)

    override fun updateStatusBarColor(value: Int) {
        val drawable = drawer.statusBarBackgroundDrawable
        if (drawable is ColorDrawable) {
            drawable.color = value
            drawer.setStatusBarBackground(drawable)
        } else {
            drawer.setStatusBarBackgroundColor(value)
        }
    }
}

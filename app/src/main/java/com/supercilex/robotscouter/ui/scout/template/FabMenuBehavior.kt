package com.supercilex.robotscouter.ui.scout.template

import android.content.Context
import android.support.annotation.Keep
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.Snackbar
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.view.View

import com.github.clans.fab.FloatingActionMenu

class FabMenuBehavior : CoordinatorLayout.Behavior<FloatingActionMenu> {
    @Keep constructor() : super()

    @Keep constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun layoutDependsOn(
            parent: CoordinatorLayout?,
            child: FloatingActionMenu?,
            dependency: View?): Boolean {
        return dependency is Snackbar.SnackbarLayout
    }

    override fun onDependentViewChanged(
            parent: CoordinatorLayout?,
            child: FloatingActionMenu?,
            dependency: View?): Boolean {
        if (dependency is Snackbar.SnackbarLayout) {
            ViewCompat.setTranslationY(
                    child, Math.min(0f, ViewCompat.getTranslationY(dependency) - dependency.height))
            return true
        } else {
            return super.onDependentViewChanged(parent, child, dependency)
        }
    }

    override fun onDependentViewRemoved(
            parent: CoordinatorLayout?,
            child: FloatingActionMenu?,
            dependency: View?) {
        if (dependency is Snackbar.SnackbarLayout) {
            ViewCompat.setTranslationY(
                    child, Math.max(0f, ViewCompat.getTranslationY(dependency) - dependency.height))
        }
    }
}

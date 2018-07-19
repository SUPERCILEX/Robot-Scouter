package com.supercilex.robotscouter.feature.teams

import androidx.recyclerview.selection.SelectionTracker

internal abstract class AllChangesSelectionObserver<T> : SelectionTracker.SelectionObserver<T>() {
    override fun onSelectionRefresh() = onSelectionChanged()

    override fun onSelectionRestored() = onSelectionChanged()
}

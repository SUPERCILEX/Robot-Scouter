package com.supercilex.robotscouter.feature.teams

import android.view.View
import androidx.recyclerview.selection.SelectionTracker
import com.firebase.ui.firestore.ObservableSnapshotArray
import com.google.android.material.snackbar.Snackbar
import com.supercilex.robotscouter.core.ui.AllChangesSelectionObserver

internal class SnackbarSelectionObserver(
        private val rootView: View,
        private val tracker: SelectionTracker<String>,
        private val items: ObservableSnapshotArray<*>
) : AllChangesSelectionObserver<String>() {
    private var _selectAllSnackbar: Snackbar? = null
    private var selectAllSnackbar: Snackbar
        get() {
            return _selectAllSnackbar ?: snackbar().also { _selectAllSnackbar = it }
        }
        set(value) {
            _selectAllSnackbar = value
        }

    override fun onSelectionChanged() {
        val isSnackbarShown = selectAllSnackbar.isShown
        val selection = tracker.selection

        if (selection.size() in 2 until items.size) {
            if (!isSnackbarShown) selectAllSnackbar.show()
        } else if (isSnackbarShown) {
            selectAllSnackbar.dismiss()
            // Generate a new SnackBar since a user dismissed one can't be shown again.
            selectAllSnackbar = snackbar()
        }
    }

    private fun snackbar(): Snackbar = Snackbar.make(
            rootView,
            R.string.team_multiple_selected_message,
            Snackbar.LENGTH_INDEFINITE
    ).setAction(R.string.team_select_all_title) {
        tracker.setItemsSelected(items.mapIndexed { index, _ -> items.getSnapshot(index).id }, true)
    }
}

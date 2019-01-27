package com.supercilex.robotscouter.feature.trash

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.children
import androidx.lifecycle.LiveData
import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.RecyclerView
import com.supercilex.robotscouter.core.ui.ItemDetailsBase
import com.supercilex.robotscouter.core.ui.ItemDetailsLookupBase
import com.supercilex.robotscouter.core.ui.ToolbarMenuHelperBase
import org.jetbrains.anko.find
import com.supercilex.robotscouter.R as RC

internal class TrashKeyProvider(
        private val observable: LiveData<List<Trash>?>
) : ItemKeyProvider<String>(SCOPE_MAPPED) {
    override fun getKey(position: Int) = observable.value?.get(position)?.id

    override fun getPosition(key: String) =
            observable.value.orEmpty().indexOfFirst { it.id == key }
}

internal class TrashDetailsLookup(recyclerView: RecyclerView) :
        ItemDetailsLookupBase<String, TrashViewHolder, TrashDetails>(recyclerView, ::TrashDetails)

internal class TrashDetails(
        private val holder: TrashViewHolder
) : ItemDetailsBase<String, TrashViewHolder>(holder) {
    override fun getSelectionKey() = holder.trash.id
}

internal class TrashMenuHelper(
        private val fragment: TrashFragment,
        tracker: SelectionTracker<String>
) : ToolbarMenuHelperBase<String>(fragment.requireActivity() as AppCompatActivity, tracker) {
    private val toolbar = fragment.requireActivity().find<Toolbar>(RC.id.toolbar)
    private val defaultNavIcon = toolbar.navigationIcon

    private val normalMenuItems = mutableListOf<MenuItem>()
    private val selectedMenuItems = mutableListOf<MenuItem>()

    override fun handleNavigationClick(hasSelection: Boolean) {
        if (!hasSelection) fragment.requireActivity().onBackPressed()
    }

    override fun createMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.trash_options, menu)

        normalMenuItems.clear()
        normalMenuItems.addAll(menu.children.filterNot { selectedMenuIds.contains(it.itemId) })
        selectedMenuItems.clear()
        selectedMenuItems.addAll(menu.children.filter { selectedMenuIds.contains(it.itemId) })
    }

    override fun handleItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_restore_all, R.id.action_restore -> fragment.restoreItems()
            R.id.action_delete -> fragment.emptySelected()
            else -> return false
        }
        return true
    }

    override fun updateNormalMenu(visible: Boolean) {
        for (item in normalMenuItems) item.isVisible = visible
    }

    override fun updateMultiSelectMenu(visible: Boolean) {
        for (item in selectedMenuItems) item.isVisible = visible
    }

    override fun updateNavigationIcon(default: Boolean) {
        if (default) {
            toolbar.navigationIcon = defaultNavIcon
        } else {
            toolbar.setNavigationIcon(R.drawable.ic_close_colorable_24dp)
        }
    }

    fun onTrashCountUpdate(hasTrash: Boolean) {
        for (item in normalMenuItems) item.isVisible = hasTrash
    }

    private companion object {
        val selectedMenuIds = listOf(R.id.action_restore, R.id.action_delete)
    }
}

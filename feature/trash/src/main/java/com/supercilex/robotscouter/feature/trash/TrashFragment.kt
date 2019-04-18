package com.supercilex.robotscouter.feature.trash

import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.observe
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.gms.tasks.Task
import com.supercilex.robotscouter.common.DeletionType
import com.supercilex.robotscouter.common.FIRESTORE_DELETION_QUEUE
import com.supercilex.robotscouter.core.data.model.untrashTeam
import com.supercilex.robotscouter.core.data.model.untrashTemplate
import com.supercilex.robotscouter.core.ui.AllChangesSelectionObserver
import com.supercilex.robotscouter.core.ui.FragmentBase
import com.supercilex.robotscouter.core.ui.KeyboardShortcutListener
import com.supercilex.robotscouter.core.ui.animatePopReveal
import com.supercilex.robotscouter.shared.stateViewModels
import kotlinx.android.synthetic.main.fragment_trash.*
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.find
import org.jetbrains.anko.support.v4.longToast
import com.supercilex.robotscouter.R as RC

internal class TrashFragment : FragmentBase(R.layout.fragment_trash), View.OnClickListener,
        KeyboardShortcutListener {
    private val holder by stateViewModels<TrashHolder>()
    private val allItems get() = holder.trashListener.value.orEmpty()

    private lateinit var selectionTracker: SelectionTracker<String>
    private lateinit var menuHelper: TrashMenuHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        holder.init()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        actionEmptyTrash.setOnClickListener(this)
        (activity as AppCompatActivity).apply {
            setSupportActionBar(find(RC.id.toolbar))
            checkNotNull(supportActionBar).setDisplayHomeAsUpEnabled(true)
        }

        val divider = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        divider.setDrawable(checkNotNull(AppCompatResources.getDrawable(
                requireContext(), R.drawable.trash_item_divider)))
        trashList.addItemDecoration(divider)

        val adapter = TrashListAdapter()
        trashList.adapter = adapter

        selectionTracker = SelectionTracker.Builder<String>(
                FIRESTORE_DELETION_QUEUE,
                trashList,
                TrashKeyProvider(holder.trashListener),
                TrashDetailsLookup(trashList),
                StorageStrategy.createStringStorage()
        ).build().apply {
            adapter.selectionTracker = this
            addObserver(TrashMenuHelper(this@TrashFragment, this).also { menuHelper = it })

            val backPressedCallback = requireActivity().onBackPressedDispatcher
                    .addCallback(viewLifecycleOwner, false) { clearSelection() }
            addObserver(object : AllChangesSelectionObserver<String>() {
                override fun onSelectionChanged() {
                    backPressedCallback.isEnabled = hasSelection()
                }
            })

            onRestoreInstanceState(savedInstanceState)
        }

        holder.trashListener.observe(viewLifecycleOwner) {
            val hasTrash = it.orEmpty().isNotEmpty()

            noTrashHint.animatePopReveal(!hasTrash)
            notice.isVisible = hasTrash
            menuHelper.onTrashCountUpdate(hasTrash)

            if (it != null) for (i in 0 until adapter.itemCount) {
                // Unselect deleted items
                val item = adapter.getItem(i)
                if (!it.contains(item)) selectionTracker.deselect(item.id)
            }
            adapter.submitList(it)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        selectionTracker.onSaveInstanceState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menuHelper.onCreateOptionsMenu(menu, inflater)
        menuHelper.onTrashCountUpdate(holder.trashListener.value.orEmpty().isNotEmpty())
    }

    override fun onOptionsItemSelected(item: MenuItem) = menuHelper.onOptionsItemSelected(item)

    override fun onShortcut(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DEL, KeyEvent.KEYCODE_FORWARD_DEL ->
                if (selectionTracker.hasSelection()) emptySelected() else emptyAll()
            else -> return false
        }
        return true
    }

    override fun onClick(v: View) {
        emptyAll()
    }

    fun restoreItems() {
        val all = allItems
        val restored = selectionTracker.selection.toList().map { key ->
            all.single { it.id == key }
        }.ifEmpty { all }.onEach { (id, _, type) ->
            when (type) {
                DeletionType.TEAM -> untrashTeam(id)
                DeletionType.TEMPLATE -> untrashTemplate(id)
                else -> error("Unsupported type: $type")
            }
        }

        if (restored.isNotEmpty()) {
            requireView().longSnackbar(resources.getQuantityString(
                    R.plurals.trash_restored_message, restored.size, restored.size))
        }
    }

    fun emptySelected() {
        showEmptyTrashDialog(selectionTracker.selection.toList())
    }

    fun onEmptyTrashConfirmed(deleted: List<String>, emptyTrashResult: Task<*>) {
        requireView().longSnackbar(resources.getQuantityString(
                R.plurals.trash_emptied_message, deleted.size, deleted.size))

        // Visual hack to pretend items were deleted really fast (usually takes a few seconds)
        val data = holder.trashListener as MutableLiveData
        val prev = data.value
        data.value = prev.orEmpty().filter { !deleted.contains(it.id) }

        emptyTrashResult.addOnFailureListener(requireActivity()) {
            longToast(R.string.trash_empty_failed_message)
            data.value = prev
        }
    }

    private fun emptyAll() {
        showEmptyTrashDialog(allItems.map { it.id }, true)
    }

    private fun showEmptyTrashDialog(ids: List<String>, emptyAll: Boolean = false) {
        EmptyTrashDialog.show(childFragmentManager, ids, emptyAll)
    }

    companion object {
        const val TAG = "TrashFragment"

        fun newInstance() = TrashFragment()
    }
}

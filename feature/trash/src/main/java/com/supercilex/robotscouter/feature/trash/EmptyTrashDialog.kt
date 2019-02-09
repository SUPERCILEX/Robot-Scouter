package com.supercilex.robotscouter.feature.trash

import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import com.supercilex.robotscouter.core.data.emptyTrash
import com.supercilex.robotscouter.core.ui.DialogFragmentBase
import com.supercilex.robotscouter.core.unsafeLazy

internal class EmptyTrashDialog : DialogFragmentBase(), DialogInterface.OnClickListener {
    private val ids by unsafeLazy { requireArguments().getStringArray(IDS_KEY).orEmpty().toList() }
    private val emptyAll by unsafeLazy { requireArguments().getBoolean(EMPTY_ALL_KEY) }

    override fun onCreateDialog(savedInstanceState: Bundle?) = AlertDialog.Builder(requireContext())
            .setTitle(R.string.trash_empty_dialog_title)
            .setMessage(resources.getQuantityString(
                    R.plurals.trash_empty_dialog_message, ids.size, ids.size))
            .setPositiveButton(R.string.trash_empty_dialog_action, this)
            .setNegativeButton(android.R.string.cancel, null)
            .create()

    override fun onClick(dialog: DialogInterface, which: Int) {
        (parentFragment as TrashFragment)
                .onEmptyTrashConfirmed(ids, emptyTrash(if (emptyAll) null else ids))
    }

    companion object {
        private const val TAG = "EmptyTrashDialog"
        private const val IDS_KEY = "ids"
        private const val EMPTY_ALL_KEY = "empty_all"

        fun show(manager: FragmentManager, ids: List<String>, emptyAll: Boolean) {
            require(ids.isNotEmpty())
            EmptyTrashDialog().apply {
                arguments = bundleOf(IDS_KEY to ids.toTypedArray(), EMPTY_ALL_KEY to emptyAll)
            }.show(manager, TAG)
        }
    }
}

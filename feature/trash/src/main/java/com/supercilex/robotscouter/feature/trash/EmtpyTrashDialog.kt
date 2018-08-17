package com.supercilex.robotscouter.feature.trash

import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import com.supercilex.robotscouter.core.data.emptyTrash
import com.supercilex.robotscouter.core.ui.DialogFragmentBase
import com.supercilex.robotscouter.core.unsafeLazy

internal class EmtpyTrashDialog : DialogFragmentBase(), DialogInterface.OnClickListener {
    private val ids by unsafeLazy { checkNotNull(arguments?.getStringArray(IDS_KEY)).toList() }
    private val emptyAll by unsafeLazy { checkNotNull(arguments).getBoolean(EMPTY_ALL_KEY) }

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
        private const val TAG = "EmtpyTrashDialog"
        private const val IDS_KEY = "ids"
        private const val EMPTY_ALL_KEY = "empty_all"

        fun show(manager: FragmentManager, ids: List<String>, emptyAll: Boolean) {
            require(ids.isNotEmpty())
            EmtpyTrashDialog().apply {
                arguments = bundleOf(IDS_KEY to ids.toTypedArray(), EMPTY_ALL_KEY to emptyAll)
            }.show(manager, TAG)
        }
    }
}

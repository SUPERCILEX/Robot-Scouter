package com.supercilex.robotscouter.shared

import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentManager
import com.supercilex.robotscouter.core.data.shouldShowRatingDialog
import com.supercilex.robotscouter.core.ui.DialogFragmentBase
import com.supercilex.robotscouter.core.ui.showStoreListing

class RatingDialog : DialogFragmentBase(), DialogInterface.OnClickListener {
    override fun onCreateDialog(savedInstanceState: Bundle?) = AlertDialog.Builder(requireContext())
            .setTitle(R.string.rate_title)
            .setMessage(R.string.rate_message)
            .setPositiveButton(R.string.rate_positive, this)
            .setNegativeButton(R.string.rate_negative, this)
            .setNeutralButton(R.string.rate_neutral, this)
            .create()

    override fun onClick(dialog: DialogInterface, which: Int) {
        shouldShowRatingDialog = when (which) {
            DialogInterface.BUTTON_POSITIVE -> {
                requireActivity().showStoreListing()
                false
            }
            DialogInterface.BUTTON_NEGATIVE -> false
            else -> true
        }
    }

    companion object {
        private const val TAG = "RatingDialog"

        fun show(manager: FragmentManager) = RatingDialog().show(manager, TAG)
    }
}

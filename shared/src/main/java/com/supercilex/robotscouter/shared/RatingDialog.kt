package com.supercilex.robotscouter.shared

import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.support.v7.app.AlertDialog
import com.supercilex.robotscouter.core.data.shouldShowRatingDialog
import com.supercilex.robotscouter.core.ui.DialogFragmentBase

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
                UpdateDialog.showStoreListing(requireActivity())
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

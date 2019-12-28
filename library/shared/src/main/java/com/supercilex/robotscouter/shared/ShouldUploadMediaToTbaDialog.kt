package com.supercilex.robotscouter.shared

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.supercilex.robotscouter.core.data.shouldAskToUploadMediaToTba
import com.supercilex.robotscouter.core.data.shouldUploadMediaToTba
import com.supercilex.robotscouter.core.isInTestMode
import com.supercilex.robotscouter.core.ui.DialogFragmentBase
import kotlinx.android.synthetic.main.dialog_should_upload_media.*

class ShouldUploadMediaToTbaDialog : DialogFragmentBase(), DialogInterface.OnClickListener {
    override fun onCreateDialog(savedInstanceState: Bundle?) = AlertDialog.Builder(requireContext())
            .setTitle(R.string.media_should_upload_title)
            .setMessage(getText(R.string.media_should_upload_rationale).trim())
            .setView(R.layout.dialog_should_upload_media)
            .setPositiveButton(R.string.yes, this)
            .setNegativeButton(R.string.no, this)
            .create()

    override fun onStart() {
        super.onStart()
        requireDialog().findViewById<TextView>(android.R.id.message).movementMethod =
                LinkMovementMethod.getInstance()
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        val isYes: Boolean = which == Dialog.BUTTON_POSITIVE

        if (requireDialog().save.isChecked) shouldUploadMediaToTba = isYes
        (parentFragment as CaptureTeamMediaListener).startCapture(isYes)
    }

    companion object {
        private const val TAG = "ShouldUploadMediaToTbaD"

        fun <F> show(
                fragment: F
        ) where F : Fragment, F : CaptureTeamMediaListener {
            if (isInTestMode) {
                fragment.startCapture(false)
                return
            }

            if (shouldAskToUploadMediaToTba) {
                ShouldUploadMediaToTbaDialog().show(fragment.childFragmentManager, TAG)
            } else {
                fragment.startCapture(shouldUploadMediaToTba)
            }
        }
    }
}

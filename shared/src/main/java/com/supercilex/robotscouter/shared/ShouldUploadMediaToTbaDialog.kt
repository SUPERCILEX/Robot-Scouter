package com.supercilex.robotscouter.shared

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.TextView
import com.supercilex.robotscouter.core.data.shouldAskToUploadMediaToTba
import com.supercilex.robotscouter.core.data.shouldUploadMediaToTba
import com.supercilex.robotscouter.core.ui.DialogFragmentBase
import com.supercilex.robotscouter.core.ui.create
import com.supercilex.robotscouter.core.unsafeLazy
import kotlinx.android.synthetic.main.dialog_should_upload_media.*
import org.jetbrains.anko.support.v4.find

class ShouldUploadMediaToTbaDialog : DialogFragmentBase(), DialogInterface.OnClickListener {
    override val containerView: View by unsafeLazy {
        View.inflate(context, R.layout.dialog_should_upload_media, null)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) = AlertDialog.Builder(requireContext())
            .setTitle(R.string.media_should_upload_title)
            .setMessage(R.string.media_should_upload_rationale)
            .setView(containerView)
            .setPositiveButton(R.string.yes, this)
            .setNegativeButton(R.string.no, this)
            .create {
                find<TextView>(android.R.id.message).movementMethod =
                        LinkMovementMethod.getInstance()
            }

    override fun onClick(dialog: DialogInterface, which: Int) {
        val isYes: Boolean = which == Dialog.BUTTON_POSITIVE

        if (save.isChecked) shouldUploadMediaToTba = isYes
        (parentFragment as CaptureTeamMediaListener).startCapture(isYes)
    }

    companion object {
        private const val TAG = "ShouldUploadMediaToTbaD"

        fun <F> show(
                fragment: F
        ) where F : Fragment, F : CaptureTeamMediaListener = if (shouldAskToUploadMediaToTba) {
            ShouldUploadMediaToTbaDialog().show(fragment.childFragmentManager, TAG)
        } else {
            fragment.startCapture(shouldUploadMediaToTba)
        }
    }
}

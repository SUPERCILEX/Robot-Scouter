package com.supercilex.robotscouter.ui

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.util.setShouldAskToUploadMediaToTba
import com.supercilex.robotscouter.util.shouldAskToUploadMediaToTba

class ShouldUploadMediaToTbaDialog : DialogFragment(), DialogInterface.OnClickListener {
    private val rootView: View by lazy {
        View.inflate(context, R.layout.dialog_should_upload_media, null)
    }
    private val saveResponseCheckbox: CheckBox by lazy {
        rootView.findViewById<CheckBox>(R.id.save_response)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = AlertDialog.Builder(context)
            .setTitle(R.string.should_upload_media_dialog_title)
            .setMessage(R.string.should_upload_media_rationale)
            .setView(rootView)
            .setPositiveButton(R.string.yes, this)
            .setNegativeButton(R.string.no, this)
            .createAndListen {
                (findViewById(android.R.id.message) as TextView).movementMethod =
                        LinkMovementMethod.getInstance()
            }

    override fun onClick(dialog: DialogInterface, which: Int) {
        val isYes: Boolean = which == Dialog.BUTTON_POSITIVE
        setShouldAskToUploadMediaToTba(context, !saveResponseCheckbox.isChecked to isYes)
        (parentFragment as TeamMediaCreator.StartCaptureListener).onStartCapture(isYes)
    }

    companion object {
        private val TAG = "ShouldUploadMediaToTbaD"

        fun show(fragment: Fragment) {
            val uploadMediaToTbaPair: Pair<Boolean, Boolean> = shouldAskToUploadMediaToTba(fragment.context)
            if (uploadMediaToTbaPair.first) {
                ShouldUploadMediaToTbaDialog().show(fragment.childFragmentManager, TAG)
            } else {
                (fragment as TeamMediaCreator.StartCaptureListener)
                        .onStartCapture(uploadMediaToTbaPair.second)
            }
        }
    }
}

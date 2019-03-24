package com.supercilex.robotscouter.feature.templates

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentManager
import com.google.firebase.firestore.DocumentReference
import com.supercilex.robotscouter.core.data.getRef
import com.supercilex.robotscouter.core.data.model.trashTemplate
import com.supercilex.robotscouter.core.data.putRef
import com.supercilex.robotscouter.core.ui.ManualDismissDialog
import com.supercilex.robotscouter.core.ui.show
import com.supercilex.robotscouter.R as RC

internal class DeleteTemplateDialog : ManualDismissDialog() {
    override fun onCreateDialog(savedInstanceState: Bundle?) = AlertDialog.Builder(requireContext())
            .setTitle(R.string.template_delete_dialog_title)
            .setMessage(R.string.template_delete_message)
            .setPositiveButton(RC.string.delete, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()

    override fun onAttemptDismiss(): Boolean {
        trashTemplate(requireArguments().getRef().id)
        return true
    }

    companion object {
        private const val TAG = "DeleteTemplateDialog"

        fun show(manager: FragmentManager, templateRef: DocumentReference) =
                DeleteTemplateDialog().show(manager, TAG) { putRef(templateRef) }
    }
}

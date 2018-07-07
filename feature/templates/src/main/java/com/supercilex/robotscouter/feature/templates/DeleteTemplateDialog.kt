package com.supercilex.robotscouter.feature.templates

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentManager
import com.google.firebase.firestore.DocumentReference
import com.supercilex.robotscouter.common.FIRESTORE_TEMPLATE_ID
import com.supercilex.robotscouter.core.data.defaultTemplateId
import com.supercilex.robotscouter.core.data.firestoreBatch
import com.supercilex.robotscouter.core.data.getRef
import com.supercilex.robotscouter.core.data.model.ref
import com.supercilex.robotscouter.core.data.model.trashTemplate
import com.supercilex.robotscouter.core.data.putRef
import com.supercilex.robotscouter.core.data.teams
import com.supercilex.robotscouter.core.data.waitForChange
import com.supercilex.robotscouter.core.logFailures
import com.supercilex.robotscouter.core.model.TemplateType
import com.supercilex.robotscouter.core.ui.ManualDismissDialog
import com.supercilex.robotscouter.core.ui.show
import kotlinx.coroutines.experimental.async

internal class DeleteTemplateDialog : ManualDismissDialog() {
    override fun onCreateDialog(savedInstanceState: Bundle?) = AlertDialog.Builder(requireContext())
            .setTitle(R.string.template_delete_dialog_title)
            .setMessage(R.string.template_delete_message)
            .setPositiveButton(R.string.template_delete_title, null)
            .setNegativeButton(android.R.string.cancel, null)
            .createAndSetup(savedInstanceState)

    override fun onAttemptDismiss(): Boolean {
        val deletedTemplateId = checkNotNull(arguments).getRef().id
        val newTemplateId = defaultTemplateId
        async {
            val teamRefs = teams.waitForChange().filter {
                deletedTemplateId == it.templateId
            }.map { it.ref }
            firestoreBatch {
                for (ref in teamRefs) update(ref, FIRESTORE_TEMPLATE_ID, newTemplateId)
            }.logFailures(teamRefs, newTemplateId)

            if (deletedTemplateId == newTemplateId) {
                defaultTemplateId = TemplateType.DEFAULT.id.toString()
            }

            trashTemplate(deletedTemplateId)
        }.logFailures()
        return true
    }

    companion object {
        private const val TAG = "DeleteTemplateDialog"

        fun show(manager: FragmentManager, templateRef: DocumentReference) =
                DeleteTemplateDialog().show(manager, TAG) { putRef(templateRef) }
    }
}

package com.supercilex.robotscouter.ui.scouting.templatelist

import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.support.v7.app.AlertDialog
import com.google.firebase.firestore.DocumentReference
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.TemplateType
import com.supercilex.robotscouter.util.FIRESTORE_TEMPLATE_ID
import com.supercilex.robotscouter.util.data.TeamsLiveData
import com.supercilex.robotscouter.util.data.defaultTemplateId
import com.supercilex.robotscouter.util.data.firestoreBatch
import com.supercilex.robotscouter.util.data.getRef
import com.supercilex.robotscouter.util.data.model.ref
import com.supercilex.robotscouter.util.data.model.trashTemplate
import com.supercilex.robotscouter.util.data.observeOnDataChanged
import com.supercilex.robotscouter.util.data.observeOnce
import com.supercilex.robotscouter.util.data.putRef
import com.supercilex.robotscouter.util.data.safeCopy
import com.supercilex.robotscouter.util.logFailures
import com.supercilex.robotscouter.util.ui.ManualDismissDialog
import com.supercilex.robotscouter.util.ui.show
import kotlinx.coroutines.experimental.async

class DeleteTemplateDialog : ManualDismissDialog() {
    override fun onCreateDialog(savedInstanceState: Bundle?) = AlertDialog.Builder(requireContext())
            .setTitle(R.string.template_delete_dialog_title)
            .setMessage(R.string.template_delete_message)
            .setPositiveButton(R.string.template_delete_title, null)
            .setNegativeButton(android.R.string.cancel, null)
            .createAndSetup(savedInstanceState)

    override fun onAttemptDismiss(): Boolean {
        val deletedTemplateId = arguments!!.getRef().id
        val newTemplateId = defaultTemplateId
        async {
            val teamRefs = TeamsLiveData.observeOnDataChanged().observeOnce()!!.safeCopy().filter {
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

package com.supercilex.robotscouter.ui.scouting.templatelist

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.support.v7.app.AlertDialog
import com.google.android.gms.tasks.Tasks
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
import com.supercilex.robotscouter.util.log
import com.supercilex.robotscouter.util.logFailures
import com.supercilex.robotscouter.util.ui.ManualDismissDialog
import com.supercilex.robotscouter.util.ui.show

class DeleteTemplateDialog : ManualDismissDialog() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = AlertDialog.Builder(context!!)
            .setTitle(R.string.template_delete_dialog_title)
            .setMessage(R.string.template_delete_message)
            .setPositiveButton(R.string.template_delete_title, null)
            .setNegativeButton(android.R.string.cancel, null)
            .createAndSetup(savedInstanceState)

    override fun onAttemptDismiss(): Boolean {
        val templateId = arguments!!.getRef().id
        TeamsLiveData.observeOnDataChanged().observeOnce { teams ->
            firestoreBatch {
                teams.filter { templateId == it.templateId }.forEach {
                    update(it.ref.log(), FIRESTORE_TEMPLATE_ID, defaultTemplateId)
                }
            }.logFailures()

            if (templateId == defaultTemplateId) {
                defaultTemplateId = TemplateType.DEFAULT.id.toString()
            }
            trashTemplate(templateId)

            Tasks.forResult(null)
        }
        return true
    }

    companion object {
        private const val TAG = "DeleteTemplateDialog"

        fun show(manager: FragmentManager, templateRef: DocumentReference) =
                DeleteTemplateDialog().show(manager, TAG) { putRef(templateRef) }
    }
}

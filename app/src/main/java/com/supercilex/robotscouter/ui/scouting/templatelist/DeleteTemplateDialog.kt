package com.supercilex.robotscouter.ui.scouting.templatelist

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.support.v7.app.AlertDialog
import android.text.TextUtils
import com.google.firebase.database.DatabaseReference
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.DEFAULT_TEMPLATE_TYPE
import com.supercilex.robotscouter.util.FIREBASE_TEMPLATES
import com.supercilex.robotscouter.util.FIREBASE_TEMPLATE_KEY
import com.supercilex.robotscouter.util.data.defaultTemplateKey
import com.supercilex.robotscouter.util.data.getRef
import com.supercilex.robotscouter.util.data.getRefBundle
import com.supercilex.robotscouter.util.data.model.ref
import com.supercilex.robotscouter.util.data.model.templateIndicesRef
import com.supercilex.robotscouter.util.data.observeOnDataChanged
import com.supercilex.robotscouter.util.data.observeOnce
import com.supercilex.robotscouter.util.teamsListener
import com.supercilex.robotscouter.util.ui.ManualDismissDialog
import com.supercilex.robotscouter.util.ui.show

class DeleteTemplateDialog : ManualDismissDialog() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = AlertDialog.Builder(context)
            .setTitle(R.string.confirm_action)
            .setMessage(R.string.delete_template_warning)
            .setPositiveButton(R.string.delete_template, null)
            .setNegativeButton(android.R.string.cancel, null)
            .createAndSetup()

    override fun onAttemptDismiss(): Boolean {
        val templateKey = getRef(arguments).key
        teamsListener.observeOnDataChanged().observeOnce().addOnSuccessListener { teams ->
            (0 until teams.size)
                    .map { teams.getObject(it) }
                    .filter { TextUtils.equals(templateKey, it.templateKey) }
                    .forEach { it.ref.child(FIREBASE_TEMPLATE_KEY).removeValue() }

            if (templateKey == defaultTemplateKey) defaultTemplateKey = DEFAULT_TEMPLATE_TYPE
            templateIndicesRef.child(templateKey).removeValue()
            FIREBASE_TEMPLATES.child(templateKey).removeValue()
        }

        return true
    }

    companion object {
        private const val TAG = "DeleteTemplateDialog"

        fun show(manager: FragmentManager, templateRef: DatabaseReference) =
                DeleteTemplateDialog().show(manager, TAG, getRefBundle(templateRef))
    }
}

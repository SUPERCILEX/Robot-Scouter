package com.supercilex.robotscouter.ui.scout.template

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.support.v7.app.AlertDialog

import com.google.firebase.database.DatabaseReference
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.util.DatabaseHelper

class RemoveAllMetricsDialog : DialogFragment(), DialogInterface.OnClickListener {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = AlertDialog.Builder(context)
            .setTitle(R.string.confirm_action)
            .setPositiveButton(R.string.remove_metrics, this)
            .setNegativeButton(android.R.string.cancel, null)
            .create()

    override fun onClick(dialog: DialogInterface, which: Int) {
        DatabaseHelper.getRef(arguments).removeValue()
    }

    companion object {
        private val TAG = "RemoveAllMetricsDialog"

        fun show(manager: FragmentManager, templateRef: DatabaseReference) {
            val fragment = RemoveAllMetricsDialog()
            fragment.arguments = DatabaseHelper.getRefBundle(templateRef)
            fragment.show(manager, TAG)
        }
    }
}

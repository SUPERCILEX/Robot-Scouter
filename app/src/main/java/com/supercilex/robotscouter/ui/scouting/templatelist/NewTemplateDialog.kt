package com.supercilex.robotscouter.ui.scouting.templatelist

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.FragmentManager
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.TEMPLATE_TYPES
import com.supercilex.robotscouter.util.data.model.addTemplate
import com.supercilex.robotscouter.util.ui.DialogFragmentBase

class NewTemplateDialog : DialogFragmentBase(), DialogInterface.OnClickListener {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = AlertDialog.Builder(context)
            .setTitle(R.string.title_new_template)
            .setItems(R.array.new_template_options, this)
            .setNegativeButton(android.R.string.cancel, null)
            .create()

    override fun onClick(dialog: DialogInterface, which: Int) {
        (parentFragment as TemplateListFragment).onTemplateCreated(addTemplate(run {
            when {
                TEMPLATE_TYPES.contains(which) -> which
                else -> throw IllegalStateException("Unknown template type: $which")
            }
        }))
    }

    companion object {
        private const val TAG = "NewTemplateDialog"

        fun show(manager: FragmentManager) = NewTemplateDialog().show(manager, TAG)
    }
}

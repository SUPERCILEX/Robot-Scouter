package com.supercilex.robotscouter.feature.templates

import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentManager
import com.supercilex.robotscouter.core.data.model.addTemplate
import com.supercilex.robotscouter.core.model.TemplateType
import com.supercilex.robotscouter.core.ui.DialogFragmentBase
import com.supercilex.robotscouter.R as RC

internal class NewTemplateDialog : DialogFragmentBase(), DialogInterface.OnClickListener {
    override fun onCreateDialog(savedInstanceState: Bundle?) = AlertDialog.Builder(requireContext())
            .setTitle(R.string.template_new_title)
            .setItems(RC.array.template_new_options, this)
            .setNegativeButton(android.R.string.cancel, null)
            .create()

    override fun onClick(dialog: DialogInterface, which: Int) {
        (requireParentFragment() as TemplateListFragment)
                .onTemplateCreated(addTemplate(TemplateType.valueOf(which)))
    }

    companion object {
        private const val TAG = "NewTemplateDialog"

        fun show(manager: FragmentManager) = NewTemplateDialog().show(manager, TAG)
    }
}

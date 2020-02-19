package com.supercilex.robotscouter.feature.templates

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.supercilex.robotscouter.core.ui.BottomSheetDialogFragmentBase
import com.supercilex.robotscouter.core.ui.LifecycleAwareLazy
import com.supercilex.robotscouter.feature.templates.databinding.AddMetricDialogBinding

internal class AddMetricDialog : BottomSheetDialogFragmentBase(), View.OnClickListener {
    private val binding by LifecycleAwareLazy {
        AddMetricDialogBinding.bind(requireDialog().findViewById(R.id.root))
    }

    override fun onDialogCreated(dialog: Dialog, savedInstanceState: Bundle?) {
        dialog.setContentView(R.layout.add_metric_dialog)

        BottomSheetBehavior.from(
                dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet)
        ).state = BottomSheetBehavior.STATE_EXPANDED
        listOf(
                binding.addHeader,
                binding.addCheckbox,
                binding.addStopwatch,
                binding.addNote,
                binding.addCounter,
                binding.addSpinner
        ).forEach {
            it.setOnClickListener(this)
        }
    }

    override fun onClick(v: View) {
        (requireParentFragment() as View.OnClickListener).onClick(v)
        dismiss()
    }

    companion object {
        private const val TAG = "AddMetricDialog"

        fun show(manager: FragmentManager) = AddMetricDialog().show(manager, TAG)
    }
}

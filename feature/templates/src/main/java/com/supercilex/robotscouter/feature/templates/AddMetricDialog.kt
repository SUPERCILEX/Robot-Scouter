package com.supercilex.robotscouter.feature.templates

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.supercilex.robotscouter.core.ui.BottomSheetDialogFragmentBase
import com.supercilex.robotscouter.core.unsafeLazy
import kotlinx.android.synthetic.main.dialog_add_metric.*
import org.jetbrains.anko.find

internal class AddMetricDialog : BottomSheetDialogFragmentBase(), View.OnClickListener {
    override val containerView: View by unsafeLazy {
        View.inflate(context, R.layout.dialog_add_metric, null)
    }

    override fun onDialogCreated(dialog: Dialog, savedInstanceState: Bundle?) {
        BottomSheetBehavior.from(dialog.find(com.google.android.material.R.id.design_bottom_sheet))
                .state = BottomSheetBehavior.STATE_EXPANDED
        listOf(addHeader, addCheckBox, addStopwatch, addNote, addCounter, addSpinner).forEach {
            it.setOnClickListener(this)
        }
    }

    override fun onClick(v: View) {
        (parentFragment as View.OnClickListener).onClick(v)
        dismiss()
    }

    companion object {
        private const val TAG = "AddMetricDialog"

        fun show(manager: FragmentManager) = AddMetricDialog().show(manager, TAG)
    }
}

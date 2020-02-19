package com.supercilex.robotscouter.shared.scouting

import android.os.Bundle
import androidx.annotation.StringRes
import com.supercilex.robotscouter.core.ui.KeyboardDialogBase
import com.supercilex.robotscouter.core.ui.LifecycleAwareLazy
import com.supercilex.robotscouter.core.unsafeLazy
import com.supercilex.robotscouter.shared.scouting.databinding.ValueDialogBinding

internal abstract class ValueDialogBase<out T> : KeyboardDialogBase() {
    private val binding by LifecycleAwareLazy {
        ValueDialogBinding.bind(requireDialog().findViewById(R.id.root))
    }

    override val lastEditText by unsafeLazy { binding.valueView }

    protected abstract val value: T?
    @get:StringRes protected abstract val title: Int
    @get:StringRes protected abstract val hint: Int

    override fun onCreateDialog(savedInstanceState: Bundle?) =
            createDialog(R.layout.value_dialog, title)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val hintText = getString(hint)
        val dialog = requireDialog()
        dialog.setOnShowListener {
            binding.valueLayout.hint = hintText
            lastEditText.apply {
                setText(requireArguments().getString(CURRENT_VALUE))
                if (savedInstanceState == null) post { selectAll() }
            }
        }
    }

    protected companion object {
        const val CURRENT_VALUE = "current_value"
    }
}

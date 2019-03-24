package com.supercilex.robotscouter.shared.scouting

import android.os.Bundle
import android.widget.EditText
import androidx.annotation.StringRes
import com.supercilex.robotscouter.core.ui.KeyboardDialogBase
import com.supercilex.robotscouter.core.unsafeLazy
import kotlinx.android.synthetic.main.dialog_value.*

internal abstract class ValueDialogBase<out T> : KeyboardDialogBase() {
    override val lastEditText: EditText by unsafeLazy { requireDialog().valueView }

    protected abstract val value: T?
    @get:StringRes protected abstract val title: Int
    @get:StringRes protected abstract val hint: Int

    override fun onCreateDialog(savedInstanceState: Bundle?) =
            createDialog(R.layout.dialog_value, title)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val hintText = getString(hint)
        val dialog = requireDialog()
        dialog.setOnShowListener {
            dialog.valueLayout.hint = hintText
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

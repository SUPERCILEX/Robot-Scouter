package com.supercilex.robotscouter.ui.scouting

import android.content.DialogInterface
import android.os.Bundle
import android.support.annotation.StringRes
import android.view.View
import android.widget.EditText
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.util.ui.KeyboardDialogBase
import com.supercilex.robotscouter.util.unsafeLazy
import kotlinx.android.synthetic.main.dialog_value.*

abstract class ValueDialogBase<out T> : KeyboardDialogBase() {
    override val containerView: View by unsafeLazy {
        View.inflate(context, R.layout.dialog_value, null)
    }
    override val lastEditText by unsafeLazy { valueView as EditText }

    protected abstract val value: T?
    @get:StringRes protected abstract val title: Int
    @get:StringRes protected abstract val hint: Int

    override fun onCreateDialog(savedInstanceState: Bundle?) =
            createDialog(title, savedInstanceState)

    override fun onShow(dialog: DialogInterface, savedInstanceState: Bundle?) {
        super.onShow(dialog, savedInstanceState)
        valueLayout.hint = getString(hint)
        lastEditText.apply {
            setText(arguments!!.getString(CURRENT_VALUE))
            if (savedInstanceState == null) post { selectAll() }
        }
    }

    protected companion object {
        const val CURRENT_VALUE = "current_value"
    }
}

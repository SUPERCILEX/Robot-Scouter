package com.supercilex.robotscouter.ui.scout

import android.app.Dialog
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.design.widget.TextInputLayout
import android.view.View
import android.widget.EditText

import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.ui.KeyboardDialogBase
import com.supercilex.robotscouter.util.DatabaseHelper

abstract class ScoutValueDialogBase<out T> : KeyboardDialogBase() {
    private val rootView: View by lazy { View.inflate(context, R.layout.dialog_scout_value, null) }
    protected val inputLayout: TextInputLayout by lazy { rootView.findViewById<TextInputLayout>(R.id.value_layout) }
    override val lastEditText: EditText by lazy { inputLayout.findViewById<EditText>(R.id.value) }

    protected abstract val value: T?
    @get:StringRes protected abstract val title: Int
    @get:StringRes protected abstract val hint: Int

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        inputLayout.hint = getString(hint)
        lastEditText.apply {
            setText(arguments.getString(CURRENT_VALUE))
            post { selectAll() }
        }

        return createDialog(rootView, title)
    }

    override fun onDestroy() {
        super.onDestroy()
        RobotScouter.getRefWatcher(activity).watch(this)
    }

    override fun onClick(): Boolean {
        DatabaseHelper.getRef(arguments).setValue(value)
        return true
    }

    protected companion object {
        @JvmStatic val CURRENT_VALUE = "current_value"
    }
}

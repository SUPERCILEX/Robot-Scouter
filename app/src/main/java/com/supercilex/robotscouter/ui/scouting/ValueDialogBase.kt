package com.supercilex.robotscouter.ui.scouting

import android.app.Dialog
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.design.widget.TextInputLayout
import android.view.View
import android.widget.EditText
import com.google.firebase.crash.FirebaseCrash
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.util.data.getRef
import com.supercilex.robotscouter.util.ui.KeyboardDialogBase

abstract class ValueDialogBase<out T> : KeyboardDialogBase() {
    private val rootView: View by lazy { View.inflate(context, R.layout.dialog_value, null) }
    protected val inputLayout: TextInputLayout by lazy { rootView.findViewById<TextInputLayout>(R.id.value_layout) }
    override val lastEditText: EditText by lazy { inputLayout.findViewById<EditText>(R.id.value) }

    protected abstract val value: T?
    @get:StringRes protected abstract val title: Int
    @get:StringRes protected abstract val hint: Int

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        inputLayout.hint = getString(hint)
        lastEditText.apply {
            setText(arguments.getString(CURRENT_VALUE))
            if (savedInstanceState == null) post { selectAll() }
        }

        return createDialog(rootView, title)
    }

    override fun onAttemptDismiss(): Boolean {
        getRef(arguments).setValue(value).addOnFailureListener { FirebaseCrash.report(it) }
        return true
    }

    protected companion object {
        const val CURRENT_VALUE = "current_value"
    }
}

package com.supercilex.robotscouter.util.ui

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.annotation.CallSuper
import android.support.annotation.StringRes
import android.support.design.widget.BottomSheetDialog
import android.support.design.widget.BottomSheetDialogFragment
import android.support.design.widget.CoordinatorLayout
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.support.v7.app.AlertDialog
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import com.google.firebase.analytics.FirebaseAnalytics
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.util.refWatcher
import java.lang.reflect.Field

inline fun AlertDialog.Builder.create(crossinline listener: AlertDialog.() -> Unit): AlertDialog =
        create().apply { setOnShowListener { (it as AlertDialog).listener() } }

fun DialogFragment.show(
        manager: FragmentManager,
        tag: String,
        args: Bundle = Bundle(),
        argsListener: (Bundle.() -> Unit)? = null
) {
    arguments = args.apply { argsListener?.invoke(this) }
    show(manager, tag)
}

abstract class DialogFragmentBase : DialogFragment() {
    override fun onResume() {
        super.onResume()
        FirebaseAnalytics.getInstance(context)
                .setCurrentScreen(activity!!, null, javaClass.simpleName)
    }

    override fun onDestroy() {
        super.onDestroy()
        refWatcher.watch(this)
    }
}

abstract class BottomSheetDialogFragmentBase : BottomSheetDialogFragment() {
    override fun onCreateDialog(
            savedInstanceState: Bundle?
    ): Dialog = object : BottomSheetDialog(context!!, R.style.RobotScouter_Tmp_72076683),
            DialogInterface.OnShowListener {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            onDialogCreated(this, savedInstanceState)
            setOnShowListener(this)
        }

        override fun onStart() {
            // Save state
            behavior.apply {
                val old = behavior.get(dialog) as CoordinatorLayout.Behavior<*>?
                behavior.set(dialog, null)
                super.onStart()
                behavior.set(dialog, old)
            }
        }

        override fun onShow(dialog: DialogInterface) {
            val width = context.resources.getDimensionPixelSize(R.dimen.bottom_sheet_width)
            window.setLayout(if (width > 0) {
                width
            } else {
                ViewGroup.LayoutParams.MATCH_PARENT
            }, ViewGroup.LayoutParams.MATCH_PARENT)
        }
    }

    open fun onDialogCreated(dialog: Dialog, savedInstanceState: Bundle?) = Unit

    override fun onResume() {
        super.onResume()
        FirebaseAnalytics.getInstance(context)
                .setCurrentScreen(activity!!, null, javaClass.simpleName)
    }

    override fun onDestroy() {
        super.onDestroy()
        refWatcher.watch(this)
    }

    companion object {
        val behavior: Field = BottomSheetDialog::class.java.getDeclaredField("mBehavior").apply {
            isAccessible = true
        }
    }
}

/**
 * Enables choosing whether or not to dismiss the dialog when the positive button is clicked.
 *
 * **Note:** for this class to work correctly, the dialog must be an [AlertDialog] and set a
 * [DialogInterface.OnShowListener].
 */
abstract class ManualDismissDialog : DialogFragmentBase() {
    /** @return true if the dialog should be dismissed, false otherwise */
    protected abstract fun onAttemptDismiss(): Boolean

    protected fun AlertDialog.Builder.createAndSetup(savedInstanceState: Bundle?) =
            create { onShow(this, savedInstanceState) }

    @CallSuper
    open fun onShow(dialog: DialogInterface, savedInstanceState: Bundle?) {
        dialog as AlertDialog
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            handleOnAttemptDismiss()
        }
    }

    protected fun handleOnAttemptDismiss() {
        if (onAttemptDismiss()) dismiss()
    }
}

abstract class KeyboardDialogBase : ManualDismissDialog(), TextView.OnEditorActionListener {
    protected abstract val lastEditText: EditText

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog.window.setKeyboardModeVisible()
    }

    protected fun createDialog(rootView: View, @StringRes title: Int, savedInstanceState: Bundle?) =
            AlertDialog.Builder(context!!)
                    .setView(rootView)
                    .setTitle(title)
                    .setPositiveButton(android.R.string.ok, null)
                    .setNegativeButton(android.R.string.cancel, null)
                    .createAndSetup(savedInstanceState)

    override fun onShow(dialog: DialogInterface, savedInstanceState: Bundle?) {
        super.onShow(dialog, savedInstanceState)
        lastEditText.setOnEditorActionListener(this)
    }

    override fun onEditorAction(v: TextView, actionId: Int, event: KeyEvent?): Boolean {
        if (event?.keyCode == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE) {
            handleOnAttemptDismiss()
            return true
        }
        return false
    }
}

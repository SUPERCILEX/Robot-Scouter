package com.supercilex.robotscouter.core.ui

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
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.google.firebase.analytics.FirebaseAnalytics
import com.supercilex.robotscouter.core.refWatcher
import java.lang.reflect.Field

inline fun AlertDialog.Builder.create(crossinline listener: AlertDialog.() -> Unit): AlertDialog =
        create().apply { setOnShowListener { (it as AlertDialog).listener() } }

fun DialogFragment.show(
        manager: FragmentManager,
        tag: String,
        args: Bundle = Bundle.EMPTY,
        argsListener: (Bundle.() -> Unit)? = null
) {
    arguments = args.apply { argsListener?.invoke(this) }
    show(manager, tag)
}

abstract class DialogFragmentBase : DialogFragment() {
    protected open val containerView: View? = null

    override fun getView() = containerView

    override fun onResume() {
        super.onResume()
        FirebaseAnalytics.getInstance(requireContext())
                .setCurrentScreen(requireActivity(), null, javaClass.simpleName)
    }

    override fun onDestroy() {
        super.onDestroy()
        refWatcher.watch(this)
    }
}

abstract class BottomSheetDialogFragmentBase : BottomSheetDialogFragment(),
        DialogInterface.OnShowListener {
    protected abstract val containerView: View

    override fun onCreateDialog(
            savedInstanceState: Bundle?
    ): Dialog = object : BottomSheetDialog(requireContext(), R.style.RobotScouter_Tmp_72076683),
            DialogInterface.OnShowListener {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            onDialogCreated(this, savedInstanceState)
            setOnShowListener(this)
        }

        override fun onStart() {
            // Save state TODO remove after https://issuetracker.google.com/issues/72125225
            behavior.apply {
                val old = get(dialog) as CoordinatorLayout.Behavior<*>?
                set(dialog, null)
                super.onStart()
                set(dialog, old)
            }
        }

        override fun onShow(dialog: DialogInterface) {
            val width = context.resources.getDimensionPixelSize(R.dimen.bottom_sheet_width)
            window.setLayout(if (width > 0) {
                width
            } else {
                ViewGroup.LayoutParams.MATCH_PARENT
            }, ViewGroup.LayoutParams.MATCH_PARENT)

            this@BottomSheetDialogFragmentBase.onShow(dialog)
        }
    }

    override fun getView() = containerView

    open fun onDialogCreated(dialog: Dialog, savedInstanceState: Bundle?) = Unit

    override fun onShow(dialog: DialogInterface) = Unit

    override fun onResume() {
        super.onResume()
        FirebaseAnalytics.getInstance(requireContext())
                .setCurrentScreen(requireActivity(), null, javaClass.simpleName)
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

abstract class KeyboardDialogBase : ManualDismissDialog() {
    protected abstract val lastEditText: EditText

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog.window.setKeyboardModeVisible()
    }

    protected fun createDialog(@StringRes title: Int, savedInstanceState: Bundle?) =
            AlertDialog.Builder(requireContext())
                    .setView(view)
                    .setTitle(title)
                    .setPositiveButton(android.R.string.ok, null)
                    .setNegativeButton(android.R.string.cancel, null)
                    .createAndSetup(savedInstanceState)

    override fun onShow(dialog: DialogInterface, savedInstanceState: Bundle?) {
        super.onShow(dialog, savedInstanceState)
        lastEditText.setImeOnDoneListener { handleOnAttemptDismiss() }
    }
}

package com.supercilex.robotscouter.core.ui

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.analytics.FirebaseAnalytics

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
        val screenName = javaClass.simpleName
        FirebaseAnalytics.getInstance(requireContext())
                .setCurrentScreen(requireActivity(), screenName, screenName)
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

        override fun onShow(dialog: DialogInterface) {
            val width = context.resources.getDimensionPixelSize(R.dimen.bottom_sheet_width)
            window?.setLayout(if (width > 0) {
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
        val screenName = javaClass.simpleName
        FirebaseAnalytics.getInstance(requireContext())
                .setCurrentScreen(requireActivity(), screenName, screenName)
    }
}

/**
 * Enables choosing whether or not to dismiss the dialog when the positive button is clicked.
 *
 * **Note:** for this class to work correctly, the dialog must be an [AlertDialog].
 */
abstract class ManualDismissDialog : DialogFragmentBase() {
    /** @return true if the dialog should be dismissed, false otherwise */
    protected abstract fun onAttemptDismiss(): Boolean

    override fun onStart() {
        super.onStart()
        (requireDialog() as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
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
        dialog?.window?.setKeyboardModeVisible()
    }

    override fun onStart() {
        super.onStart()
        lastEditText.apply {
            setImeOnDoneListener { handleOnAttemptDismiss() }
            requestFocus()
            showKeyboard()
        }
    }

    protected fun createDialog(
            @LayoutRes viewId: Int,
            @StringRes title: Int
    ) = AlertDialog.Builder(requireContext())
            .setView(viewId)
            .setTitle(title)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
}

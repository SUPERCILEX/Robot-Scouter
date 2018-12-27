package com.supercilex.robotscouter

import android.content.DialogInterface
import android.os.Bundle
import android.text.Html
import androidx.appcompat.app.AlertDialog
import androidx.core.text.parseAsHtml
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import com.supercilex.robotscouter.core.data.updateRequiredMessage
import com.supercilex.robotscouter.core.ui.DialogFragmentBase
import com.supercilex.robotscouter.core.ui.showStoreListing

internal class UpdateDialog : DialogFragmentBase(), DialogInterface.OnClickListener {
    override fun onCreateDialog(savedInstanceState: Bundle?) = AlertDialog.Builder(requireContext())
            .setTitle(R.string.update_required_title)
            .setMessage(updateRequiredMessage.parseAsHtml(Html.FROM_HTML_MODE_COMPACT))
            .setPositiveButton(R.string.update_title, this)
            .setOnCancelListener(this)
            .setCancelable(false)
            .create()

    override fun onClick(dialog: DialogInterface, which: Int) = requireActivity().showStoreListing()

    override fun onCancel(dialog: DialogInterface) = requireActivity().finish()

    companion object {
        private const val TAG = "UpdateDialog"

        fun show(manager: FragmentManager) {
            if (manager.findFragmentByTag(TAG) == null) manager.commit(allowStateLoss = true) {
                add(UpdateDialog(), TAG)
            }
        }
    }
}

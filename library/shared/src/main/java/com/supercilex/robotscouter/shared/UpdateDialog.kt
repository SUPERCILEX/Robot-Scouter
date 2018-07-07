package com.supercilex.robotscouter.shared

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.Html
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.core.text.parseAsHtml
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.transaction
import com.supercilex.robotscouter.core.data.updateRequiredMessage
import com.supercilex.robotscouter.core.ui.DialogFragmentBase

class UpdateDialog : DialogFragmentBase(), DialogInterface.OnClickListener {
    override fun onCreateDialog(savedInstanceState: Bundle?) = AlertDialog.Builder(requireContext())
            .setTitle(R.string.update_required_title)
            .setMessage(updateRequiredMessage.parseAsHtml(Html.FROM_HTML_MODE_COMPACT))
            .setPositiveButton(R.string.update_title, this)
            .setOnCancelListener(this)
            .setCancelable(false)
            .create()

    override fun onClick(dialog: DialogInterface, which: Int) = showStoreListing(requireActivity())

    override fun onCancel(dialog: DialogInterface?) = requireActivity().finish()

    companion object {
        private const val TAG = "UpdateDialog"

        private val STORE_LISTING_URI = "market://details?id=com.supercilex.robotscouter".toUri()
        private val LATEST_APK_URI =
                "https://github.com/SUPERCILEX/app-version-history/blob/master/Robot-Scouter/app-release.aab".toUri()

        fun show(manager: FragmentManager) {
            if (manager.findFragmentByTag(TAG) == null) manager.transaction(allowStateLoss = true) {
                add(UpdateDialog(), TAG)
            }
        }

        fun showStoreListing(activity: Activity) = try {
            activity.startActivity(Intent(Intent.ACTION_VIEW).setData(STORE_LISTING_URI))
        } catch (e: ActivityNotFoundException) {
            activity.startActivity(Intent(Intent.ACTION_VIEW).setData(LATEST_APK_URI))
        }
    }
}

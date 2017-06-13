package com.supercilex.robotscouter.ui.teamlist

import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.support.v7.app.AlertDialog

import com.supercilex.robotscouter.BuildConfig
import com.supercilex.robotscouter.R

class UpdateDialog : DialogFragment(), DialogInterface.OnClickListener {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = AlertDialog.Builder(context)
            .setTitle(R.string.update_required_title)
            .setMessage(R.string.update_required_message)
            .setPositiveButton(R.string.update, this)
            .setOnCancelListener(this)
            .setCancelable(false)
            .create()

    override fun onClick(dialog: DialogInterface, which: Int) = showStoreListing(activity)

    override fun onCancel(dialog: DialogInterface?) = activity.finish()

    companion object {
        private const val TAG = "UpdateDialog"
        private val STORE_LISTING_URI: Uri = Uri.parse("market://details?id=${BuildConfig.APPLICATION_ID}")
        private val LATEST_APK_URI: Uri = Uri.parse("https://github.com/SUPERCILEX/app-version-history/blob/master/Robot-Scouter/app-release.apk")

        fun show(manager: FragmentManager) {
            if (manager.findFragmentByTag(TAG) == null) {
                manager.beginTransaction().add(UpdateDialog(), TAG).commitAllowingStateLoss()
            }
        }

        fun showStoreListing(activity: Activity) = try {
            activity.startActivity(Intent(Intent.ACTION_VIEW).setData(STORE_LISTING_URI))
        } catch (e: ActivityNotFoundException) {
            activity.startActivity(Intent(Intent.ACTION_VIEW).setData(LATEST_APK_URI))
        }
    }
}

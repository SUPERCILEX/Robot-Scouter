package com.supercilex.robotscouter.ui.teamlist

import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.support.v7.app.AlertDialog
import android.text.Html
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.supercilex.robotscouter.BuildConfig
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.util.ui.DialogFragmentBase

class UpdateDialog : DialogFragmentBase(), DialogInterface.OnClickListener {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = AlertDialog.Builder(context)
            .setTitle(R.string.update_required_title)
            .setMessage(fun(): CharSequence {
                val message = FirebaseRemoteConfig.getInstance().getString(UPDATE_MESSAGE_KEY)
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Html.fromHtml(message, Html.FROM_HTML_MODE_COMPACT)
                } else {
                    @Suppress("DEPRECATION")
                    Html.fromHtml(message)
                }
            }.invoke())
            .setPositiveButton(R.string.update, this)
            .setOnCancelListener(this)
            .setCancelable(false)
            .create()

    override fun onClick(dialog: DialogInterface, which: Int) = showStoreListing(activity)

    override fun onCancel(dialog: DialogInterface?) = activity.finish()

    companion object {
        private const val TAG = "UpdateDialog"
        private const val UPDATE_MESSAGE_KEY = "update_required_message"

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

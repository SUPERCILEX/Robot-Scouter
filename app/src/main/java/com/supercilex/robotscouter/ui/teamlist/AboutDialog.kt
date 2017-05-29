package com.supercilex.robotscouter.ui.teamlist

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.support.v7.app.AlertDialog
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.TextView
import android.widget.Toast

import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.util.getDebugInfo

class AboutDialog : DialogFragment(), DialogInterface.OnClickListener {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val rootView = View.inflate(context, R.layout.about, null) as TextView
        rootView.movementMethod = LinkMovementMethod.getInstance()
        return AlertDialog.Builder(context)
                .setView(rootView)
                .setTitle(R.string.about)
                .setPositiveButton(android.R.string.ok, null)
                .setNeutralButton(R.string.copy_debug_info, this)
                .create()
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip = ClipData.newPlainText(
                getString(R.string.debug_info_name),
                getDebugInfo())
        Toast.makeText(context, R.string.debug_info_copied, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "AboutDialog"

        fun show(manager: FragmentManager) = AboutDialog().show(manager, TAG)
    }
}

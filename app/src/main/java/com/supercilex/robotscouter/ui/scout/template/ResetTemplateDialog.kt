package com.supercilex.robotscouter.ui.scout.template

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.support.v7.app.AlertDialog
import android.text.TextUtils
import android.view.View
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.data.util.TeamHelper
import com.supercilex.robotscouter.data.util.UserHelper
import com.supercilex.robotscouter.util.FIREBASE_SCOUT_TEMPLATES
import com.supercilex.robotscouter.util.FIREBASE_TEMPLATE_KEY
import com.supercilex.robotscouter.util.create
import com.supercilex.robotscouter.util.show

class ResetTemplateDialog : DialogFragment(), View.OnClickListener {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = AlertDialog.Builder(context)
            .setTitle(R.string.confirm_action)
            .setPositiveButton(R.string.reset, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create {
                getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(this@ResetTemplateDialog)
            }

    override fun dismiss() {
        super.dismiss()
        (parentFragment as DialogFragment).dismiss()
    }

    override fun onDestroy() {
        super.onDestroy()
        RobotScouter.getRefWatcher(activity).watch(this)
    }

    override fun onClick(v: View) {
        val team: Team = TeamHelper.parse(arguments).team
        val templateKey: String = team.templateKey

        if (arguments.getBoolean(RESET_ALL_KEY)) {
            Constants.sFirebaseTeams
                    .map { it.child(FIREBASE_TEMPLATE_KEY) }
                    .filter { TextUtils.equals(templateKey, it.getValue(String::class.java)) }
                    .forEach { it.ref.removeValue() }

            UserHelper.getScoutTemplateIndicesRef().child(templateKey).removeValue()
            FIREBASE_SCOUT_TEMPLATES.child(templateKey).removeValue()
        } else {
            team.helper.ref.child(FIREBASE_TEMPLATE_KEY).removeValue()
        }

        dismiss()
    }

    companion object {
        private const val TAG = "ResetTemplateDialog"
        private const val RESET_ALL_KEY = "reset_all_key"

        fun show(manager: FragmentManager, helper: TeamHelper, shouldResetAll: Boolean) =
                ResetTemplateDialog().show(manager, TAG, helper.toBundle()) {
                    putBoolean(RESET_ALL_KEY, shouldResetAll)
                }
    }
}

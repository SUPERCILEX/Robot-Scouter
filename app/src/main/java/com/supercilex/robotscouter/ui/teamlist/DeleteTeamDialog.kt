package com.supercilex.robotscouter.ui.teamlist

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.support.v7.app.AlertDialog
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.util.TeamHelper
import com.supercilex.robotscouter.util.Constants
import java.util.*

class DeleteTeamDialog : DialogFragment(), DialogInterface.OnClickListener {
    private val mTeamHelpers by lazy { TeamHelper.parseList(arguments) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Collections.sort(mTeamHelpers)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val deletedTeams = StringBuilder()
        for (i in mTeamHelpers.indices) {
            deletedTeams.append(i + 1)
                    .append(". ")
                    .append(mTeamHelpers[i])
                    .append('\n')
        }

        return AlertDialog.Builder(context)
                .setTitle(R.string.confirm_action)
                .setMessage(when {
                    mTeamHelpers.size == Constants.SINGLE_ITEM -> null
                    else -> getString(R.string.caution_delete, deletedTeams)
                })
                .setPositiveButton(R.string.delete, this)
                .setNegativeButton(android.R.string.cancel, null)
                .create()
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        for (teamHelper in mTeamHelpers) teamHelper.deleteTeam()
    }

    companion object {
        private val TAG = "DeleteTeamDialog"

        fun show(manager: FragmentManager, teamHelpers: List<TeamHelper>) {
            val dialog = DeleteTeamDialog()
            dialog.arguments = TeamHelper.toBundle(teamHelpers)
            dialog.show(manager, TAG)
        }
    }
}

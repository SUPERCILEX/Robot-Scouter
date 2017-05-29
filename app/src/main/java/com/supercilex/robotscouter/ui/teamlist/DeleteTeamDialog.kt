package com.supercilex.robotscouter.ui.teamlist

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.support.v7.app.AlertDialog
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.util.TeamHelper
import com.supercilex.robotscouter.util.SINGLE_ITEM
import com.supercilex.robotscouter.util.show
import java.util.*

class DeleteTeamDialog : DialogFragment(), DialogInterface.OnClickListener {
    private val teamHelpers: List<TeamHelper> by lazy { TeamHelper.parseList(arguments) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Collections.sort(teamHelpers)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val deletedTeams = StringBuilder()
        for ((i: Int, teamHelper: TeamHelper) in teamHelpers.withIndex()) {
            deletedTeams.append(i + 1)
                    .append(". ")
                    .append(teamHelper)
                    .append('\n')
        }

        return AlertDialog.Builder(context)
                .setTitle(R.string.confirm_action)
                .setMessage(when (SINGLE_ITEM) {
                    teamHelpers.size -> null
                    else -> getString(R.string.caution_delete, deletedTeams)
                })
                .setPositiveButton(R.string.delete, this)
                .setNegativeButton(android.R.string.cancel, null)
                .create()
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        for (teamHelper: TeamHelper in teamHelpers) teamHelper.deleteTeam()
    }

    companion object {
        private const val TAG = "DeleteTeamDialog"

        fun show(manager: FragmentManager, teamHelpers: List<TeamHelper>) =
                DeleteTeamDialog().show(manager, TAG, TeamHelper.toBundle(teamHelpers))
    }
}

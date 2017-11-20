package com.supercilex.robotscouter.ui.teamlist

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.support.v7.app.AlertDialog
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.data.getTeamList
import com.supercilex.robotscouter.util.data.model.trash
import com.supercilex.robotscouter.util.data.toBundle
import com.supercilex.robotscouter.util.isSingleton
import com.supercilex.robotscouter.util.ui.DialogFragmentBase
import com.supercilex.robotscouter.util.ui.show
import com.supercilex.robotscouter.util.unsafeLazy
import java.util.Collections

class DeleteTeamDialog : DialogFragmentBase(), DialogInterface.OnClickListener {
    private val teams: List<Team> by unsafeLazy { arguments!!.getTeamList() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Collections.sort(teams)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val deletedTeams = StringBuilder()
        for ((i: Int, team: Team) in teams.withIndex()) {
            deletedTeams.append(i + 1)
                    .append(". ")
                    .append(team)
                    .append('\n')
        }

        return AlertDialog.Builder(context!!)
                .setTitle(R.string.team_delete_dialog_title)
                .setMessage(if (teams.isSingleton) {
                    null
                } else {
                    getString(R.string.team_delete_rationale, deletedTeams)
                })
                .setPositiveButton(R.string.delete, this)
                .setNegativeButton(android.R.string.cancel, null)
                .create()
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        for (team: Team in teams) team.trash()
    }

    companion object {
        private const val TAG = "DeleteTeamDialog"

        fun show(manager: FragmentManager, teams: List<Team>) =
                DeleteTeamDialog().show(manager, TAG, teams.toBundle())
    }
}

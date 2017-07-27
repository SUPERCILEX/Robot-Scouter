package com.supercilex.robotscouter.ui.scouting.scout

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.support.v7.app.AlertDialog
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.data.model.deleteTeam
import com.supercilex.robotscouter.util.data.model.parseTeam
import com.supercilex.robotscouter.util.data.model.toBundle
import com.supercilex.robotscouter.util.ui.show

class ShouldDeleteTeamDialog : DialogFragment(), DialogInterface.OnClickListener {
    private val team: Team by lazy { parseTeam(arguments) }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = AlertDialog.Builder(context)
            .setTitle(R.string.should_delete_team)
            .setMessage(getString(R.string.should_delete_team_message, team.toString()))
            .setPositiveButton(R.string.delete, this)
            .setNegativeButton(R.string.no, null)
            .create()

    override fun onClick(dialog: DialogInterface, which: Int) = team.deleteTeam()

    companion object {
        private const val TAG = "ShouldDeleteTeamDialog"

        fun show(manager: FragmentManager, team: Team) =
                ShouldDeleteTeamDialog().show(manager, TAG, team.toBundle())
    }
}

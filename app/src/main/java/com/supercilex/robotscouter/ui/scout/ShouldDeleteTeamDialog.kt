package com.supercilex.robotscouter.ui.scout

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.support.v7.app.AlertDialog

import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.util.TeamHelper
import com.supercilex.robotscouter.util.show

class ShouldDeleteTeamDialog : DialogFragment(), DialogInterface.OnClickListener {
    private val mTeamHelper: TeamHelper by lazy { TeamHelper.parse(arguments) }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = AlertDialog.Builder(context)
            .setTitle(R.string.should_delete_team)
            .setMessage(getString(R.string.should_delete_team_message, mTeamHelper.toString()))
            .setPositiveButton(R.string.delete, this)
            .setNegativeButton(R.string.no, null)
            .create()

    override fun onClick(dialog: DialogInterface, which: Int) = mTeamHelper.deleteTeam()

    companion object {
        private val TAG = "ShouldDeleteTeamDialog"

        fun show(manager: FragmentManager, teamHelper: TeamHelper) =
                ShouldDeleteTeamDialog().show(manager, TAG, teamHelper.toBundle())
    }
}

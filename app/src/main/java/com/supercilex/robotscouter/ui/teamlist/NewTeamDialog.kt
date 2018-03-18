package com.supercilex.robotscouter.ui.teamlist

import android.os.Bundle
import android.support.design.widget.TextInputEditText
import android.support.v4.app.FragmentManager
import android.view.View
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.data.getScoutBundle
import com.supercilex.robotscouter.util.ui.KeyboardDialogBase
import com.supercilex.robotscouter.util.ui.TeamSelectionListener
import com.supercilex.robotscouter.util.unsafeLazy
import kotlinx.android.synthetic.main.dialog_new_team.*

class NewTeamDialog : KeyboardDialogBase() {
    override val containerView: View by unsafeLazy {
        View.inflate(context, R.layout.dialog_new_team, null)
    }
    override val lastEditText: TextInputEditText by unsafeLazy { number }

    override fun onCreateDialog(savedInstanceState: Bundle?) =
            createDialog(R.string.scout_new_title, savedInstanceState)

    public override fun onAttemptDismiss(): Boolean {
        val teamNumber = try {
            lastEditText.text.toString().toLong()
        } catch (e: NumberFormatException) {
            name.error = getString(R.string.number_too_big_error)
            return false
        }
        (activity as TeamSelectionListener)
                .onTeamSelected(getScoutBundle(Team(teamNumber, ""), true))
        return true
    }

    companion object {
        private const val TAG = "NewTeamDialog"

        fun show(manager: FragmentManager) = NewTeamDialog().show(manager, TAG)
    }
}

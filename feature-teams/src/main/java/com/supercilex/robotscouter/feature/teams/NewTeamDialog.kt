package com.supercilex.robotscouter.feature.teams

import android.os.Bundle
import android.support.design.widget.TextInputEditText
import android.support.v4.app.FragmentManager
import android.view.View
import com.supercilex.robotscouter.core.data.getScoutBundle
import com.supercilex.robotscouter.core.data.model.teamWithSafeDefaults
import com.supercilex.robotscouter.core.ui.KeyboardDialogBase
import com.supercilex.robotscouter.core.ui.TeamSelectionListener
import com.supercilex.robotscouter.core.unsafeLazy
import kotlinx.android.synthetic.main.dialog_new_team.*

class NewTeamDialog : KeyboardDialogBase() {
    override val containerView: View by unsafeLazy {
        View.inflate(context, R.layout.dialog_new_team, null)
    }
    override val lastEditText: TextInputEditText by unsafeLazy { number }

    override fun onCreateDialog(savedInstanceState: Bundle?) =
            createDialog(R.string.team_new_title, savedInstanceState)

    public override fun onAttemptDismiss(): Boolean {
        val teamNumber = try {
            lastEditText.text.toString().toLong()
        } catch (e: NumberFormatException) {
            name.error = getString(R.string.number_too_big_error)
            return false
        }
        (activity as TeamSelectionListener)
                .onTeamSelected(getScoutBundle(teamWithSafeDefaults(teamNumber, ""), true))
        return true
    }

    companion object {
        private const val TAG = "NewTeamDialog"

        fun show(manager: FragmentManager) = NewTeamDialog().show(manager, TAG)
    }
}

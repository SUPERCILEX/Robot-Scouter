package com.supercilex.robotscouter.feature.teams

import android.os.Bundle
import androidx.fragment.app.FragmentManager
import com.google.android.material.textfield.TextInputEditText
import com.supercilex.robotscouter.Bridge
import com.supercilex.robotscouter.NewTeamDialogCompanion
import com.supercilex.robotscouter.TeamSelectionListener
import com.supercilex.robotscouter.core.data.getScoutBundle
import com.supercilex.robotscouter.core.data.model.teamWithSafeDefaults
import com.supercilex.robotscouter.core.ui.KeyboardDialogBase
import com.supercilex.robotscouter.core.unsafeLazy
import kotlinx.android.synthetic.main.dialog_new_team.*
import com.supercilex.robotscouter.R as RC

@Bridge
internal class NewTeamDialog : KeyboardDialogBase() {
    override val lastEditText: TextInputEditText by unsafeLazy { requireDialog().number }

    override fun onCreateDialog(savedInstanceState: Bundle?) =
            createDialog(R.layout.dialog_new_team, RC.string.team_new_title)

    public override fun onAttemptDismiss(): Boolean {
        val teamNumber = try {
            lastEditText.text.toString().toLong()
        } catch (e: NumberFormatException) {
            requireDialog().name.error = getString(RC.string.number_too_big_error)
            return false
        }
        (activity as TeamSelectionListener)
                .onTeamSelected(getScoutBundle(teamWithSafeDefaults(teamNumber, ""), true))
        return true
    }

    companion object : NewTeamDialogCompanion {
        private const val TAG = "NewTeamDialog"

        override fun show(manager: FragmentManager) = NewTeamDialog().show(manager, TAG)
    }
}

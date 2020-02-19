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
import com.supercilex.robotscouter.core.ui.LifecycleAwareLazy
import com.supercilex.robotscouter.core.unsafeLazy
import com.supercilex.robotscouter.feature.teams.databinding.NewTeamDialogBinding
import com.supercilex.robotscouter.R as RC

@Bridge
internal class NewTeamDialog : KeyboardDialogBase() {
    private val binding by LifecycleAwareLazy {
        NewTeamDialogBinding.bind(requireDialog().findViewById(R.id.root))
    }
    override val lastEditText: TextInputEditText by unsafeLazy { binding.number }

    override fun onCreateDialog(savedInstanceState: Bundle?) =
            createDialog(R.layout.new_team_dialog, RC.string.team_new_title)

    public override fun onAttemptDismiss(): Boolean {
        val teamNumber = try {
            lastEditText.text.toString().toLong()
        } catch (e: NumberFormatException) {
            binding.name.error = getString(RC.string.number_too_big_error)
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

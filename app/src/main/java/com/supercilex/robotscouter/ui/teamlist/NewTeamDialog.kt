package com.supercilex.robotscouter.ui.teamlist

import android.os.Bundle
import android.support.design.widget.TextInputLayout
import android.support.v4.app.FragmentManager
import android.view.View
import android.widget.EditText
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.data.getScoutBundle
import com.supercilex.robotscouter.util.ui.KeyboardDialogBase
import com.supercilex.robotscouter.util.ui.TeamSelectionListener
import com.supercilex.robotscouter.util.unsafeLazy
import org.jetbrains.anko.find

class NewTeamDialog : KeyboardDialogBase() {
    private val rootView: View by unsafeLazy {
        View.inflate(context, R.layout.dialog_new_team, null)
    }
    private val inputLayout by unsafeLazy { rootView.find<TextInputLayout>(R.id.name) }
    override val lastEditText by unsafeLazy { rootView.find<EditText>(R.id.team_number) }

    override fun onCreateDialog(savedInstanceState: Bundle?) =
            createDialog(rootView, R.string.scout_new_title, savedInstanceState)

    public override fun onAttemptDismiss(): Boolean {
        val teamNumber = try {
            lastEditText.text.toString().toLong()
        } catch (e: NumberFormatException) {
            inputLayout.error = getString(R.string.number_too_big_error)
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

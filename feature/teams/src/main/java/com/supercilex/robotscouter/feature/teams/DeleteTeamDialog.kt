package com.supercilex.robotscouter.feature.teams

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.text.SpannableStringBuilder
import android.view.View
import androidx.core.text.bold
import com.supercilex.robotscouter.core.data.getTeamList
import com.supercilex.robotscouter.core.data.isSingleton
import com.supercilex.robotscouter.core.data.model.trash
import com.supercilex.robotscouter.core.data.toBundle
import com.supercilex.robotscouter.core.model.Team
import com.supercilex.robotscouter.core.ui.BottomSheetDialogFragmentBase
import com.supercilex.robotscouter.core.ui.show
import com.supercilex.robotscouter.core.unsafeLazy
import kotlinx.android.synthetic.main.dialog_delete_team.*

internal class DeleteTeamDialog : BottomSheetDialogFragmentBase(), View.OnClickListener {
    override val containerView: View by unsafeLazy {
        View.inflate(context, R.layout.dialog_delete_team, null)
    }
    private val teams: List<Team> by unsafeLazy { arguments!!.getTeamList().sorted() }

    override fun onDialogCreated(dialog: Dialog, savedInstanceState: Bundle?) {
        message.text = run {
            val message = resources.getQuantityString(
                    R.plurals.team_delete_message,
                    teams.size,
                    teams.first()
            )

            if (teams.isSingleton) return@run message

            val deletedTeams = SpannableStringBuilder(message).append('\n')
            for ((i, team) in teams.withIndex()) {
                deletedTeams.bold {
                    append('\n').append("${i + 1}. ")
                }.append(team.toString())
            }
            deletedTeams
        }
        delete.setOnClickListener(this@DeleteTeamDialog)
    }

    override fun onClick(v: View) {
        for (team in teams) team.trash()
        dismiss()
    }

    companion object {
        private const val TAG = "DeleteTeamDialog"

        fun show(manager: FragmentManager, teams: List<Team>) =
                DeleteTeamDialog().show(manager, TAG, teams.toBundle())
    }
}

package com.supercilex.robotscouter.ui.teamlist

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.text.SpannableStringBuilder
import android.view.View
import android.widget.TextView
import androidx.text.bold
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.data.getTeamList
import com.supercilex.robotscouter.util.data.model.trash
import com.supercilex.robotscouter.util.data.toBundle
import com.supercilex.robotscouter.util.isSingleton
import com.supercilex.robotscouter.util.ui.BottomSheetDialogFragmentBase
import com.supercilex.robotscouter.util.ui.show
import com.supercilex.robotscouter.util.unsafeLazy
import org.jetbrains.anko.find

class DeleteTeamDialog : BottomSheetDialogFragmentBase(), View.OnClickListener {
    private val teams: List<Team> by unsafeLazy { arguments!!.getTeamList().sorted() }

    override fun onDialogCreated(dialog: Dialog, savedInstanceState: Bundle?) {
        val view = View.inflate(context, R.layout.dialog_delete_team, null)

        view.find<TextView>(R.id.message).text = run {
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
        view.find<View>(R.id.delete).setOnClickListener(this@DeleteTeamDialog)

        dialog.setContentView(view)
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

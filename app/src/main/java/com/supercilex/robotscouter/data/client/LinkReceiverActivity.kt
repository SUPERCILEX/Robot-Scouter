package com.supercilex.robotscouter.data.client

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.google.firebase.crash.FirebaseCrash
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.data.util.TeamHelper
import com.supercilex.robotscouter.ui.scout.ScoutActivity
import com.supercilex.robotscouter.ui.scout.ScoutListFragmentBase
import com.supercilex.robotscouter.ui.scout.ScoutListFragmentBase.KEY_SCOUT_ARGS
import com.supercilex.robotscouter.ui.teamlist.TeamListActivity
import com.supercilex.robotscouter.util.KEY_QUERY
import com.supercilex.robotscouter.util.SINGLE_ITEM
import com.supercilex.robotscouter.util.addNewDocumentFlags
import com.supercilex.robotscouter.util.isTabletMode
import com.supercilex.robotscouter.util.onSignedIn

@SuppressLint("GoogleAppIndexingApiWarning")
class LinkReceiverActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onSignedIn().continueWithTask { FirebaseDynamicLinks.getInstance().getDynamicLink(intent) }
                .addOnSuccessListener {
                    val teams = getTeams(it?.link ?: intent.data ?: Uri.Builder().build())

                    if (teams.isEmpty()) {
                        showError()
                        startTeamListActivityNoArgs()
                        return@addOnSuccessListener
                    }

                    processTeams(teams)
                }
                .addOnFailureListener {
                    FirebaseCrash.report(it)
                    showError()
                    startTeamListActivityNoArgs()
                }
                .addOnCompleteListener { finish() }
    }

    private fun processTeams(teams: List<Team>) {
        for (team in teams) {
            val number = team.numberAsLong
            TeamHelper.getIndicesRef().child(team.key).setValue(number, number)
        }

        if (teams.size != SINGLE_ITEM) {
            Toast.makeText(this, R.string.teams_imported, Toast.LENGTH_LONG).show()
            startTeamListActivityNoArgs()
            return
        }

        val data = ScoutListFragmentBase.getBundle(teams[0], false, null)
        if (isTabletMode(this)) {
            startActivity(Intent(this, TeamListActivity::class.java)
                    .putExtra(KEY_SCOUT_ARGS, data)
                    .addNewDocumentFlags())
        } else {
            startActivity(ScoutActivity.createIntent(this, data))
        }
    }

    private fun getTeams(link: Uri): List<Team> = link.getQueryParameters(KEY_QUERY).map {
        // Format: key:2521
        val teamPairSplit: List<String> = it.split(":")
        val teamKey = teamPairSplit[0]
        val teamNumber = teamPairSplit[1]

        Team.Builder(teamNumber).setKey(teamKey).build()
    }

    private fun startTeamListActivityNoArgs() =
            startActivity(Intent(this, TeamListActivity::class.java).addNewDocumentFlags())

    private fun showError() = Toast.makeText(this, R.string.uri_parse_error, Toast.LENGTH_LONG).show()
}

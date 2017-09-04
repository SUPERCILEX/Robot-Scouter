package com.supercilex.robotscouter.data.client

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.google.android.gms.tasks.Continuation
import com.google.firebase.crash.FirebaseCrash
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.dynamiclinks.PendingDynamicLinkData
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.ui.scouting.scoutlist.ScoutListActivity
import com.supercilex.robotscouter.ui.scouting.templatelist.TemplateListActivity
import com.supercilex.robotscouter.ui.teamlist.TeamListActivity
import com.supercilex.robotscouter.util.ACTION_FROM_DEEP_LINK
import com.supercilex.robotscouter.util.AsyncTaskExecutor
import com.supercilex.robotscouter.util.KEY_QUERY
import com.supercilex.robotscouter.util.SINGLE_ITEM
import com.supercilex.robotscouter.util.data.SCOUT_ARGS_KEY
import com.supercilex.robotscouter.util.data.getScoutBundle
import com.supercilex.robotscouter.util.data.model.teamIndicesRef
import com.supercilex.robotscouter.util.data.model.templateIndicesRef
import com.supercilex.robotscouter.util.onSignedIn
import com.supercilex.robotscouter.util.ui.addNewDocumentFlags
import com.supercilex.robotscouter.util.ui.isInTabletMode
import com.supercilex.robotscouter.util.ui.views.ContentLoadingProgressBar

@SuppressLint("GoogleAppIndexingApiWarning")
class LinkReceiverActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_link_receiver)
        findViewById<ContentLoadingProgressBar>(R.id.progress).show()

        onSignedIn().continueWithTask { FirebaseDynamicLinks.getInstance().getDynamicLink(intent) }
                .continueWith(AsyncTaskExecutor, Continuation<PendingDynamicLinkData, Unit> {
                    val link: Uri = it.result?.link ?: intent.data ?: Uri.Builder().build()

                    when (link.lastPathSegment) {
                        "teams" -> {
                            val teams = getTeams(link)

                            if (teams.isEmpty()) {
                                showErrorAndContinue()
                            } else {
                                processTeams(teams)
                            }
                        }
                        "templates" -> {
                            val templateKey = getTemplate(link)

                            if (templateKey == null) {
                                showErrorAndContinue()
                            } else {
                                processTemplate(templateKey)
                            }
                        }
                        else -> showErrorAndContinue()
                    }
                })
                .addOnFailureListener {
                    FirebaseCrash.report(it)
                    showErrorAndContinue()
                }
                .addOnCompleteListener { finish() }
    }

    private fun processTeams(teams: List<Team>) {
        for (team in teams) {
            val number = team.numberAsLong
            teamIndicesRef.child(team.key).setValue(number, number)
        }

        if (teams.size != SINGLE_ITEM) {
            Toast.makeText(this, R.string.teams_imported, Toast.LENGTH_LONG).show()
            startTeamListActivityNoArgs()
            return
        }

        val data = getScoutBundle(teams[0])
        if (isInTabletMode(this)) {
            startActivity(Intent(this, TeamListActivity::class.java)
                                  .putExtra(SCOUT_ARGS_KEY, data)
                                  .addNewDocumentFlags()
                                  .setAction(ACTION_FROM_DEEP_LINK))
        } else {
            startActivity(ScoutListActivity.createIntent(data).setAction(ACTION_FROM_DEEP_LINK))
        }
    }

    private fun processTemplate(templateKey: String) {
        templateIndicesRef.child(templateKey).setValue(true)
        startActivity(TemplateListActivity.createIntent(templateKey)
                              .addNewDocumentFlags()
                              .setAction(ACTION_FROM_DEEP_LINK))
    }

    private fun getTeams(link: Uri): List<Team> = link.getQueryParameters(KEY_QUERY).map {
        // Format: key:2521
        val teamPairSplit: List<String> = it.split(":")
        val teamKey = teamPairSplit[0]
        val teamNumber = teamPairSplit[1]

        Team(teamNumber, teamKey)
    }

    private fun getTemplate(link: Uri): String? = link.getQueryParameter(KEY_QUERY)

    private fun startTeamListActivityNoArgs() =
            startActivity(Intent(this, TeamListActivity::class.java)
                                  .addNewDocumentFlags()
                                  .setAction(ACTION_FROM_DEEP_LINK))

    private fun showErrorAndContinue() {
        Toast.makeText(this, R.string.uri_parse_error, Toast.LENGTH_LONG).show()
        startTeamListActivityNoArgs()
    }
}

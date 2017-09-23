package com.supercilex.robotscouter.data.client

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.google.android.gms.tasks.Continuation
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.dynamiclinks.PendingDynamicLinkData
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.ui.teamlist.TeamListActivity
import com.supercilex.robotscouter.util.ACTION_FROM_DEEP_LINK
import com.supercilex.robotscouter.util.AsyncTaskExecutor
import com.supercilex.robotscouter.util.CrashLogger
import com.supercilex.robotscouter.util.ID_QUERY
import com.supercilex.robotscouter.util.onSignedIn
import com.supercilex.robotscouter.util.ui.addNewDocumentFlags
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
                            val templateId = getTemplate(link)

                            if (templateId == null) {
                                showErrorAndContinue()
                            } else {
                                processTemplate(templateId)
                            }
                        }
                        else -> showErrorAndContinue()
                    }
                })
                .addOnFailureListener(CrashLogger)
                .addOnFailureListener { showErrorAndContinue() }
                .addOnCompleteListener { finish() }
    }

    private fun processTeams(teams: List<Team>) {
        TODO()
    }

    private fun processTemplate(templateId: String) {
        TODO()
    }

    private fun getTeams(link: Uri): List<Team> = link.getQueryParameters(ID_QUERY).map {
        // Format: id:2521
        val teamPairSplit: List<String> = it.split(":")
        val teamId = teamPairSplit[0]
        val teamNumber = teamPairSplit[1]

        Team(teamNumber.toLong(), teamId)
    }

    private fun getTemplate(link: Uri): String? = link.getQueryParameter(ID_QUERY)

    private fun startTeamListActivityNoArgs() =
            startActivity(Intent(this, TeamListActivity::class.java)
                                  .addNewDocumentFlags()
                                  .setAction(ACTION_FROM_DEEP_LINK))

    private fun showErrorAndContinue() {
        Toast.makeText(this, R.string.uri_parse_error, Toast.LENGTH_LONG).show()
        startTeamListActivityNoArgs()
    }
}

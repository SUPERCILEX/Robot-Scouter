package com.supercilex.robotscouter.data.client

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.annotation.WorkerThread
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Tasks
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.dynamiclinks.PendingDynamicLinkData
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.ui.scouting.scoutlist.ScoutListActivity
import com.supercilex.robotscouter.ui.scouting.templatelist.TemplateListActivity
import com.supercilex.robotscouter.ui.teamlist.TeamListActivity
import com.supercilex.robotscouter.util.AsyncTaskExecutor
import com.supercilex.robotscouter.util.CrashLogger
import com.supercilex.robotscouter.util.FIRESTORE_ACTIVE_TOKENS
import com.supercilex.robotscouter.util.data.ACTION_FROM_DEEP_LINK
import com.supercilex.robotscouter.util.data.KEYS
import com.supercilex.robotscouter.util.data.SCOUT_ARGS_KEY
import com.supercilex.robotscouter.util.data.getScoutBundle
import com.supercilex.robotscouter.util.data.updateOwner
import com.supercilex.robotscouter.util.isSingleton
import com.supercilex.robotscouter.util.onSignedIn
import com.supercilex.robotscouter.util.teams
import com.supercilex.robotscouter.util.templates
import com.supercilex.robotscouter.util.ui.addNewDocumentFlags
import com.supercilex.robotscouter.util.ui.isInTabletMode
import com.supercilex.robotscouter.util.ui.views.ContentLoadingProgressBar
import java.util.Date

class LinkReceiverActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_link_receiver)
        findViewById<ContentLoadingProgressBar>(R.id.progress).show()

        onSignedIn().continueWithTask { FirebaseDynamicLinks.getInstance().getDynamicLink(intent) }
                .continueWith(AsyncTaskExecutor, Continuation<PendingDynamicLinkData, Unit> {
                    val link: Uri = it.result?.link ?: intent.data ?: Uri.Builder().build()
                    val token: String = link.getQueryParameter(FIRESTORE_ACTIVE_TOKENS).also {
                        if (it == null) {
                            showErrorAndContinue()
                            return@Continuation
                        }
                    }

                    when (link.lastPathSegment) {
                        teams.id -> processTeams(link, token)
                        templates.id -> processTemplates(link, token)
                        else -> showErrorAndContinue()
                    }
                })
                .addOnFailureListener(CrashLogger)
                .addOnFailureListener { showErrorAndContinue() }
                .addOnCompleteListener { finish() }
    }

    @WorkerThread
    private fun processTeams(link: Uri, token: String) {
        val refs = link.getQueryParameter(KEYS).split(",").map { teams.document(it) }
        Tasks.await(updateOwner(refs, token, null) { ref ->
            link.getQueryParameter(ref.id).toLong()
        })

        if (refs.isSingleton) {
            val id = refs.single().id
            val data = getScoutBundle(Team(link.getQueryParameter(id).toLong(), id))

            if (isInTabletMode(this)) {
                startActivity(Intent(this, TeamListActivity::class.java)
                                      .putExtra(SCOUT_ARGS_KEY, data)
                                      .addNewDocumentFlags()
                                      .setAction(ACTION_FROM_DEEP_LINK))
            } else {
                startActivity(ScoutListActivity.createIntent(data).setAction(ACTION_FROM_DEEP_LINK))
            }
        } else {
            Toast.makeText(this, R.string.link_teams_imported_message, Toast.LENGTH_LONG).show()
            startTeamListActivityNoArgs()
        }
    }

    @WorkerThread
    private fun processTemplates(link: Uri, token: String) {
        val refs = link.getQueryParameter(KEYS).split(",").map { templates.document(it) }
        Tasks.await(updateOwner(refs, token, null) { Date() })

        startActivity(TemplateListActivity.createIntent(refs.single().id)
                              .addNewDocumentFlags()
                              .setAction(ACTION_FROM_DEEP_LINK))
    }

    private fun startTeamListActivityNoArgs() =
            startActivity(Intent(this, TeamListActivity::class.java)
                                  .addNewDocumentFlags()
                                  .setAction(ACTION_FROM_DEEP_LINK))

    private fun showErrorAndContinue() {
        Toast.makeText(this, R.string.link_uri_parse_error, Toast.LENGTH_LONG).show()
        startTeamListActivityNoArgs()
    }
}

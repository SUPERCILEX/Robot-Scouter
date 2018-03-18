package com.supercilex.robotscouter.data.client

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.dynamiclinks.PendingDynamicLinkData
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.ui.scouting.scoutlist.ScoutListActivity
import com.supercilex.robotscouter.ui.scouting.templatelist.TemplateListActivity
import com.supercilex.robotscouter.ui.teamlist.TeamListActivity
import com.supercilex.robotscouter.util.FIRESTORE_ACTIVE_TOKENS
import com.supercilex.robotscouter.util.await
import com.supercilex.robotscouter.util.data.ACTION_FROM_DEEP_LINK
import com.supercilex.robotscouter.util.data.KEYS
import com.supercilex.robotscouter.util.data.SCOUT_ARGS_KEY
import com.supercilex.robotscouter.util.data.getScoutBundle
import com.supercilex.robotscouter.util.data.updateOwner
import com.supercilex.robotscouter.util.isSingleton
import com.supercilex.robotscouter.util.logFailures
import com.supercilex.robotscouter.util.onSignedIn
import com.supercilex.robotscouter.util.teamsRef
import com.supercilex.robotscouter.util.templatesRef
import com.supercilex.robotscouter.util.ui.ActivityBase
import com.supercilex.robotscouter.util.ui.addNewDocumentFlags
import com.supercilex.robotscouter.util.ui.isInTabletMode
import kotlinx.android.synthetic.main.activity_link_receiver.*
import kotlinx.coroutines.experimental.async
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.longToast
import org.jetbrains.anko.runOnUiThread
import java.util.Date

@SuppressLint("GoogleAppIndexingApiWarning")
class LinkReceiverActivity : ActivityBase() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_link_receiver)
        progress.show()

        async {
            onSignedIn()

            val dynamicLink: PendingDynamicLinkData? =
                    FirebaseDynamicLinks.getInstance().getDynamicLink(intent).await()
            val link: Uri = dynamicLink?.link ?: intent.data ?: Uri.Builder().build()
            val token: String? = link.getQueryParameter(FIRESTORE_ACTIVE_TOKENS)

            when (link.lastPathSegment) {
                teamsRef.id -> processTeams(link, token)
                templatesRef.id -> processTemplates(link, token)
                else -> showErrorAndContinue()
            }
        }.logFailures().invokeOnCompletion {
            if (it != null) showErrorAndContinue()
            finish()
        }
    }

    private suspend fun processTeams(link: Uri, token: String?) {
        val refs = link.getQueryParameter(KEYS).split(",").map { teamsRef.document(it) }

        if (token != null) updateOwner(refs, token, null) { ref ->
            link.getQueryParameter(ref.id).toLong()
        }

        if (refs.isSingleton) {
            val id = refs.single().id
            val data = getScoutBundle(Team(link.getQueryParameter(id).toLong(), id))

            if (isInTabletMode()) {
                startActivity(intentFor<TeamListActivity>(SCOUT_ARGS_KEY to data)
                                      .addNewDocumentFlags()
                                      .setAction(ACTION_FROM_DEEP_LINK))
            } else {
                startActivity(ScoutListActivity.createIntent(data).setAction(ACTION_FROM_DEEP_LINK))
            }
        } else {
            RobotScouter.runOnUiThread { longToast(R.string.link_teams_imported_message) }
            startTeamListActivityNoArgs()
        }
    }

    private suspend fun processTemplates(link: Uri, token: String?) {
        val refs = link.getQueryParameter(KEYS).split(",").map { templatesRef.document(it) }

        if (token != null) updateOwner(refs, token, null) { Date() }

        startActivity(TemplateListActivity.createIntent(refs.single().id)
                              .addNewDocumentFlags()
                              .setAction(ACTION_FROM_DEEP_LINK))
    }

    private fun startTeamListActivityNoArgs() =
            startActivity(intentFor<TeamListActivity>()
                                  .addNewDocumentFlags()
                                  .setAction(ACTION_FROM_DEEP_LINK))

    private fun showErrorAndContinue() {
        startTeamListActivityNoArgs()
        RobotScouter.runOnUiThread { longToast(R.string.link_uri_parse_error) }
    }
}

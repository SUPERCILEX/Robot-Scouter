package com.supercilex.robotscouter

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.dynamiclinks.PendingDynamicLinkData
import com.supercilex.robotscouter.common.FIRESTORE_ACTIVE_TOKENS
import com.supercilex.robotscouter.common.isSingleton
import com.supercilex.robotscouter.core.await
import com.supercilex.robotscouter.core.data.ACTION_FROM_DEEP_LINK
import com.supercilex.robotscouter.core.data.KEYS
import com.supercilex.robotscouter.core.data.SCOUT_ARGS_KEY
import com.supercilex.robotscouter.core.data.TEMPLATE_ARGS_KEY
import com.supercilex.robotscouter.core.data.getScoutBundle
import com.supercilex.robotscouter.core.data.getTabIdBundle
import com.supercilex.robotscouter.core.data.model.teamWithSafeDefaults
import com.supercilex.robotscouter.core.data.teamsRef
import com.supercilex.robotscouter.core.data.templatesRef
import com.supercilex.robotscouter.core.data.updateOwner
import com.supercilex.robotscouter.core.logFailures
import com.supercilex.robotscouter.core.ui.ActivityBase
import com.supercilex.robotscouter.core.ui.addNewDocumentFlags
import com.supercilex.robotscouter.shared.client.onSignedIn
import kotlinx.android.synthetic.main.activity_link_receiver.*
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.async
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.longToast
import java.util.Date

@SuppressLint("GoogleAppIndexingApiWarning")
internal class LinkReceiverActivity : ActivityBase(), CoroutineScope {
    override val coroutineContext = Job()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_link_receiver)
        progress.show()
        handleModuleInstalls(progress)

        async {
            onSignedIn()

            val dynamicLink: PendingDynamicLinkData? =
                    FirebaseDynamicLinks.getInstance().getDynamicLink(intent).await()
            val link: Uri = dynamicLink?.link ?: intent.data ?: Uri.EMPTY
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
        val refs = checkNotNull(link.getQueryParameter(KEYS)).split(",").map {
            teamsRef.document(it)
        }

        if (token != null) updateOwner(refs, token, null) { ref ->
            checkNotNull(link.getQueryParameter(ref.id)).toLong()
        }

        if (refs.isSingleton) {
            val id = refs.single().id
            val data = getScoutBundle(teamWithSafeDefaults(
                    checkNotNull(link.getQueryParameter(id)).toLong(),
                    id
            ))

            startActivity(intentFor<HomeActivity>(SCOUT_ARGS_KEY to data)
                                  .addNewDocumentFlags()
                                  .setAction(ACTION_FROM_DEEP_LINK))
        } else {
            runOnUiThread { longToast(R.string.link_teams_imported_message) }
            startHomeActivityNoArgs()
        }
    }

    private suspend fun processTemplates(link: Uri, token: String?) {
        val refs = checkNotNull(link.getQueryParameter(KEYS)).split(",").map {
            templatesRef.document(it)
        }

        if (token != null) updateOwner(refs, token, null) { Date() }

        startActivity(intentFor<HomeActivity>(TEMPLATE_ARGS_KEY to getTabIdBundle(refs.single().id))
                              .addNewDocumentFlags()
                              .setAction(ACTION_FROM_DEEP_LINK))
    }

    private fun startHomeActivityNoArgs() =
            startActivity(intentFor<HomeActivity>()
                                  .addNewDocumentFlags()
                                  .setAction(ACTION_FROM_DEEP_LINK))

    private fun showErrorAndContinue() {
        startHomeActivityNoArgs()
        runOnUiThread { longToast(R.string.link_uri_parse_error) }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineContext.cancel()
    }
}

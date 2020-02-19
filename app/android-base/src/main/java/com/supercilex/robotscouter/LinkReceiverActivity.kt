package com.supercilex.robotscouter

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.dynamiclinks.PendingDynamicLinkData
import com.supercilex.robotscouter.common.FIRESTORE_ACTIVE_TOKENS
import com.supercilex.robotscouter.common.isSingleton
import com.supercilex.robotscouter.core.InvocationMarker
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
import com.supercilex.robotscouter.core.logBreadcrumb
import com.supercilex.robotscouter.core.longToast
import com.supercilex.robotscouter.core.ui.ActivityBase
import com.supercilex.robotscouter.core.ui.addNewDocumentFlags
import com.supercilex.robotscouter.core.unsafeLazy
import com.supercilex.robotscouter.databinding.LinkReceiverActivityBinding
import com.supercilex.robotscouter.shared.client.onSignedIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.tasks.await
import java.util.Date

@SuppressLint("GoogleAppIndexingApiWarning")
internal class LinkReceiverActivity : ActivityBase(), CoroutineScope {
    override val coroutineContext = Job()

    private val binding by unsafeLazy {
        LinkReceiverActivityBinding.bind(findViewById(R.id.progress))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.link_receiver_activity)
        binding.progress.show()
        handleModuleInstalls(binding.progress)

        async {
            val dynamicLink: PendingDynamicLinkData? =
                    FirebaseDynamicLinks.getInstance().getDynamicLink(intent).await()
            val link: Uri = dynamicLink?.link ?: intent.data ?: Uri.EMPTY
            val token: String? = link.getQueryParameter(FIRESTORE_ACTIVE_TOKENS)

            onSignedIn {
                when (link.lastPathSegment) {
                    teamsRef.id -> processTeams(link, token)
                    templatesRef.id -> processTemplates(link, token)
                    else -> showErrorAndContinue()
                }
            }
        }.invokeOnCompletion {
            if (it != null) {
                logBreadcrumb("processIncomingLinks", InvocationMarker(it))
                showErrorAndContinue()
            }
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

            startActivity(home().putExtra(SCOUT_ARGS_KEY, data)
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

        startActivity(home()
                              .putExtra(TEMPLATE_ARGS_KEY, getTabIdBundle(refs.single().id))
                              .addNewDocumentFlags()
                              .setAction(ACTION_FROM_DEEP_LINK))
    }

    private fun startHomeActivityNoArgs() =
            startActivity(home().addNewDocumentFlags().setAction(ACTION_FROM_DEEP_LINK))

    private fun showErrorAndContinue() {
        startHomeActivityNoArgs()
        runOnUiThread { longToast(R.string.link_uri_parse_error) }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineContext.cancel()
    }
}

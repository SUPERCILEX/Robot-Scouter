package com.supercilex.robotscouter.shared

import android.content.Intent
import androidx.annotation.Size
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.google.firebase.appindexing.Action
import com.google.firebase.appindexing.FirebaseUserActions
import com.google.firebase.dynamiclinks.DynamicLink
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.supercilex.robotscouter.common.isSingleton
import com.supercilex.robotscouter.core.CrashLogger
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.asLifecycleReference
import com.supercilex.robotscouter.core.data.CachingSharer
import com.supercilex.robotscouter.core.data.getTeamsLink
import com.supercilex.robotscouter.core.data.logFailures
import com.supercilex.robotscouter.core.data.logShare
import com.supercilex.robotscouter.core.data.model.TeamCache
import com.supercilex.robotscouter.core.data.model.getNames
import com.supercilex.robotscouter.core.data.model.ref
import com.supercilex.robotscouter.core.data.model.shareTeams
import com.supercilex.robotscouter.core.isOffline
import com.supercilex.robotscouter.core.logBreadcrumb
import com.supercilex.robotscouter.core.model.Team
import com.supercilex.robotscouter.core.ui.longSnackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.invoke
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TeamSharer private constructor(
        fragment: Fragment,
        @Size(min = 1) teams: List<Team>
) : CachingSharer() {
    private val cache = Cache(teams)
    private val safeMessage: String
        get() {
            val fullMessage = cache.shareMessage
            return if (fullMessage.length > MAX_MESSAGE_LENGTH) {
                val safe: (Any) -> String = {
                    RobotScouter.resources.getQuantityString(R.plurals.team_share_message, 1, it)
                }
                val first = cache.teams.first()

                if (cache.teams.isSingleton) {
                    safe(first.number).take(MAX_MESSAGE_LENGTH)
                } else {
                    val possibleSafe = safe("$first and more")
                    if (possibleSafe.length > MAX_MESSAGE_LENGTH) {
                        safe(first.number).take(MAX_MESSAGE_LENGTH)
                    } else {
                        possibleSafe
                    }
                }
            } else {
                fullMessage
            }
        }

    init {
        val fragmentRef = fragment.asLifecycleReference(fragment.viewLifecycleOwner)
        GlobalScope.launch(Dispatchers.Main) {
            try {
                val intent = Dispatchers.Default { generateIntent() }
                fragmentRef().startActivityForResult(intent, RC_SHARE)
            } catch (e: Exception) {
                CrashLogger.onFailure(e)
                fragmentRef().requireView().longSnackbar(R.string.error_unknown)
                return@launch
            }
        }
    }

    private suspend fun generateIntent(): Intent {
        val token = cache.teams.map { it.ref }.shareTeams()

        return getInvitationIntent(cache.teams.getTeamsLink(token))
    }

    private suspend fun getInvitationIntent(deepLink: String): Intent {
        val mediaUri = cache.teams.mapNotNull { it.media?.toUri() }.firstOrNull()

        val link = FirebaseDynamicLinks.getInstance().createDynamicLink()
                .setLink(deepLink.toUri())
                .setDomainUriPrefix("https://robotscouter.page.link")
                .setSocialMetaTagParameters(
                        DynamicLink.SocialMetaTagParameters.Builder()
                                .setTitle(cache.shareCta)
                                .setDescription(safeMessage)
                                .apply { mediaUri?.let { setImageUrl(it) } }
                                .build()
                )
                .buildShortDynamicLink()
                .await()
        // TODO https://github.com/firebase/firebase-android-sdk/pull/1084
        @Suppress("UselessCallOnNotNull")
        if (link.warnings.orEmpty().isNotEmpty()) {
            val warnings = link.warnings.joinToString { it.message.toString() }
            logBreadcrumb("Dynamic link warnings: $warnings")
        }

        val shareIntent = Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_SUBJECT, cache.shareCta)
                .putExtra(
                        Intent.EXTRA_TEXT,
                        cache.shareMessage + "\n\n" + link.shortLink
                )
        return Intent.createChooser(shareIntent, cache.shareTitle)
    }

    private inner class Cache(teams: Collection<Team>) : TeamCache(teams) {
        val shareMessage: String
        val shareCta: String
        val shareTitle: String

        init {
            val resources = RobotScouter.resources
            val quantity = teams.size

            shareMessage = resources.getQuantityString(
                    R.plurals.team_share_message, quantity, teamNames)
            shareCta = resources.getQuantityString(
                    R.plurals.team_share_cta, quantity, teamNames)
            shareTitle = resources.getQuantityString(
                    R.plurals.team_share_title, quantity, teamNames)
        }
    }

    companion object {
        private const val RC_SHARE = 9
        private const val MAX_MESSAGE_LENGTH = 100

        /**
         * @return true if a share intent was launched, false otherwise
         */
        fun shareTeams(fragment: Fragment, @Size(min = 1) teams: List<Team>): Boolean {
            if (isOffline) {
                fragment.requireView().longSnackbar(R.string.no_connection)
                return false
            }
            if (teams.isEmpty()) return false

            teams.logShare()
            FirebaseUserActions.getInstance().end(
                    Action.Builder(Action.Builder.SHARE_ACTION)
                            .setObject(teams.getNames(), teams.getTeamsLink())
                            .setActionStatus(Action.Builder.STATUS_TYPE_COMPLETED)
                            .build()
            ).logFailures("shareTeams:addAction")

            TeamSharer(fragment, teams)

            return true
        }
    }
}

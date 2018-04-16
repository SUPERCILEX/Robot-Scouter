package com.supercilex.robotscouter.shared

import android.content.Intent
import android.support.annotation.Size
import android.support.v4.app.Fragment
import androidx.core.net.toUri
import com.google.android.gms.appinvite.AppInviteInvitation
import com.google.android.gms.appinvite.AppInviteInvitation.IntentBuilder.MAX_MESSAGE_LENGTH
import com.google.firebase.appindexing.Action
import com.google.firebase.appindexing.FirebaseUserActions
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.SetOptions
import com.supercilex.robotscouter.common.FIRESTORE_ACTIVE_TOKENS
import com.supercilex.robotscouter.core.CrashLogger
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.asLifecycleReference
import com.supercilex.robotscouter.core.data.CachingSharer
import com.supercilex.robotscouter.core.data.QueuedDeletion
import com.supercilex.robotscouter.core.data.firestoreBatch
import com.supercilex.robotscouter.core.data.generateToken
import com.supercilex.robotscouter.core.data.getTeamsLink
import com.supercilex.robotscouter.core.data.isSingleton
import com.supercilex.robotscouter.core.data.logShare
import com.supercilex.robotscouter.core.data.model.TeamCache
import com.supercilex.robotscouter.core.data.model.getNames
import com.supercilex.robotscouter.core.data.model.ref
import com.supercilex.robotscouter.core.data.model.userDeletionQueue
import com.supercilex.robotscouter.core.isOffline
import com.supercilex.robotscouter.core.logFailures
import com.supercilex.robotscouter.core.model.Team
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.support.v4.find
import java.util.Date

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
        val fragmentRef = fragment.asLifecycleReference()
        async(UI) {
            val intent = try {
                async { generateIntent() }.await()
            } catch (e: Exception) {
                CrashLogger.onFailure(e)
                longSnackbar(fragmentRef().find(R.id.root), R.string.error_unknown)
                return@async
            }
            fragmentRef().startActivityForResult(intent, RC_SHARE)
        }.logFailures()
    }

    private suspend fun generateIntent(): Intent {
        // Called first to skip token generation if task failed
        val htmlTemplate = loadFile(FILE_NAME)

        val token = generateToken
        val tokenPath = FieldPath.of(FIRESTORE_ACTIVE_TOKENS, token)
        firestoreBatch {
            for (team in cache.teams) update(team.ref, tokenPath, Date())
            set(userDeletionQueue,
                QueuedDeletion.ShareToken.Team(token, cache.teams.map { it.id }).data,
                SetOptions.merge())
        }.logFailures(cache.teams.map { it.ref }, cache.teams)

        return getInvitationIntent(
                cache.teams.getTeamsLink(token),
                htmlTemplate.format(cache.shareCta, cache.teams.first().media)
        )
    }

    private fun getInvitationIntent(deepLink: String, shareTemplate: String) =
            AppInviteInvitation.IntentBuilder(cache.shareTitle)
                    .setMessage(safeMessage)
                    .setDeepLink(deepLink.toUri())
                    .setEmailSubject(cache.shareCta)
                    .setEmailHtmlContent(shareTemplate)
                    .build()

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
        private const val FILE_NAME = "share_team_template.html"

        /**
         * @return true if a share intent was launched, false otherwise
         */
        fun shareTeams(fragment: Fragment, @Size(min = 1) teams: List<Team>): Boolean {
            if (isOffline) {
                longSnackbar(fragment.find(R.id.root), R.string.no_connection)
                return false
            }
            if (teams.isEmpty()) return false

            teams.logShare()
            FirebaseUserActions.getInstance().end(
                    Action.Builder(Action.Builder.SHARE_ACTION)
                            .setObject(teams.getNames(), teams.getTeamsLink())
                            .setActionStatus(Action.Builder.STATUS_TYPE_COMPLETED)
                            .build()
            ).logFailures()

            TeamSharer(fragment, teams)

            return true
        }
    }
}

package com.supercilex.robotscouter.ui

import android.content.Intent
import android.net.Uri
import android.support.annotation.Size
import android.support.v4.app.FragmentActivity
import com.google.android.gms.appinvite.AppInviteInvitation
import com.google.android.gms.tasks.Continuation
import com.google.firebase.appindexing.Action
import com.google.firebase.appindexing.FirebaseUserActions
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.SetOptions
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.AsyncTaskExecutor
import com.supercilex.robotscouter.util.FIRESTORE_ACTIVE_TOKENS
import com.supercilex.robotscouter.util.data.CachingSharer
import com.supercilex.robotscouter.util.data.QueuedDeletion
import com.supercilex.robotscouter.util.data.firestoreBatch
import com.supercilex.robotscouter.util.data.generateToken
import com.supercilex.robotscouter.util.data.getTeamsLink
import com.supercilex.robotscouter.util.data.model.TeamCache
import com.supercilex.robotscouter.util.data.model.getNames
import com.supercilex.robotscouter.util.data.model.ref
import com.supercilex.robotscouter.util.data.model.userDeletionQueue
import com.supercilex.robotscouter.util.isOffline
import com.supercilex.robotscouter.util.isSingleton
import com.supercilex.robotscouter.util.log
import com.supercilex.robotscouter.util.logFailures
import com.supercilex.robotscouter.util.logIgnorableFailures
import com.supercilex.robotscouter.util.logShare
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.find
import java.util.Date
import java.util.concurrent.CancellationException

class TeamSharer private constructor(
        private val activity: FragmentActivity,
        @Size(min = 1) teams: List<Team>) : CachingSharer(activity
) {
    private val cache = Cache(teams)
    private val safeMessage: String
        get() {
            val fullMessage = cache.shareMessage
            return if (fullMessage.length > MAX_MESSAGE_LENGTH) {
                val safe: (Any) -> String = {
                    activity.resources.getQuantityString(R.plurals.team_share_message, 1, it)
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
        loadFile(FILE_NAME).continueWith(AsyncTaskExecutor, Continuation<String, Intent> {
            it.result // Skip token generation if task failed

            val token = generateToken
            val tokenPath = FieldPath.of(FIRESTORE_ACTIVE_TOKENS, token)
            for (team in cache.teams) {
                firestoreBatch {
                    update(team.ref.log(), tokenPath, Date())
                    set(userDeletionQueue.log(),
                        QueuedDeletion.ShareToken.Team(token, team.id).data,
                        SetOptions.merge())
                }.logFailures()
            }

            getInvitationIntent(
                    cache.teams.getTeamsLink(token),
                    it.result.format(cache.shareCta, cache.teams.first().media)
            )
        }).addOnSuccessListener(activity) {
            activity.startActivityForResult(it, RC_SHARE)
        }.logIgnorableFailures<Intent, CancellationException>().addOnFailureListener(activity) {
            longSnackbar(activity.find(R.id.root), R.string.fui_general_error)
        }
    }

    private fun getInvitationIntent(deepLink: String, shareTemplate: String) =
            AppInviteInvitation.IntentBuilder(cache.shareTitle)
                    .setMessage(safeMessage)
                    .setDeepLink(Uri.parse(deepLink))
                    .setEmailSubject(cache.shareCta)
                    .setEmailHtmlContent(shareTemplate)
                    .build()

    private inner class Cache(teams: Collection<Team>) : TeamCache(teams) {
        val shareMessage: String
        val shareCta: String
        val shareTitle: String

        init {
            val resources = activity.resources
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
        private const val FILE_NAME = "share_team_template.html"

        /**
         * @return true if a share intent was launched, false otherwise
         */
        fun shareTeams(activity: FragmentActivity, @Size(min = 1) teams: List<Team>): Boolean {
            if (isOffline) {
                longSnackbar(activity.find(R.id.root), R.string.no_connection)
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

            TeamSharer(activity, teams)

            return true
        }
    }
}

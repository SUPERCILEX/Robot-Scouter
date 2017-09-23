package com.supercilex.robotscouter.ui

import android.content.Intent
import android.net.Uri
import android.support.annotation.Size
import android.support.design.widget.Snackbar
import android.support.v4.app.FragmentActivity
import android.view.View
import com.google.android.gms.appinvite.AppInviteInvitation
import com.google.android.gms.tasks.Continuation
import com.google.firebase.crash.FirebaseCrash
import com.google.firebase.firestore.FieldPath
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.AsyncTaskExecutor
import com.supercilex.robotscouter.util.CrashLogger
import com.supercilex.robotscouter.util.FIRESTORE_ACTIVE_TOKENS
import com.supercilex.robotscouter.util.data.CachingSharer
import com.supercilex.robotscouter.util.data.generateToken
import com.supercilex.robotscouter.util.data.getTeamsLink
import com.supercilex.robotscouter.util.data.model.TeamCache
import com.supercilex.robotscouter.util.data.model.ref
import com.supercilex.robotscouter.util.isOffline
import com.supercilex.robotscouter.util.logShareTeamsEvent
import java.util.Date

class TeamSharer private constructor(private val activity: FragmentActivity,
                                     @Size(min = 1) teams: List<Team>) : CachingSharer(activity) {
    private val cache = Cache(teams)
    private val safeMessage: String get() {
        val fullMessage = cache.shareMessage
        return if (fullMessage.length >= MAX_MESSAGE_LENGTH) {
            activity.resources.getQuantityString(
                    R.plurals.share_team_message,
                    1,
                    "${cache.teams[0]} and more")
        } else {
            fullMessage
        }
    }

    init {
        loadFile(FILE_NAME).continueWith(AsyncTaskExecutor, Continuation<String, Intent> {
            val token = generateToken
            val tokenPath = FieldPath.of(FIRESTORE_ACTIVE_TOKENS, token)
            for (team in cache.teams) team.ref.update(tokenPath, Date())

            getInvitationIntent(
                    cache.teams.getTeamsLink(token),
                    it.result.format(cache.shareCta, cache.teams[0].media))
        }).addOnSuccessListener {
            activity.startActivityForResult(it, RC_SHARE)
        }.addOnFailureListener(CrashLogger).addOnFailureListener {
            FirebaseCrash.report(it)
            Snackbar.make(activity.findViewById<View>(R.id.root),
                          R.string.fui_general_error,
                          Snackbar.LENGTH_LONG)
                    .show()
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
                    R.plurals.share_team_message, quantity, teamNames)
            shareCta = resources.getQuantityString(
                    R.plurals.share_team_call_to_action, quantity, teamNames)
            shareTitle = resources.getQuantityString(
                    R.plurals.share_team_title, quantity, teamNames)
        }
    }

    companion object {
        private const val RC_SHARE = 9
        private const val MAX_MESSAGE_LENGTH = 100
        private const val FILE_NAME = "share_team_template.html"

        /**
         * @return true if a share intent was launched, false otherwise
         */
        fun shareTeams(activity: FragmentActivity,
                       @Size(min = 1) teams: List<Team>): Boolean {
            if (isOffline()) {
                Snackbar.make(activity.findViewById<View>(R.id.root),
                              R.string.no_connection,
                              Snackbar.LENGTH_LONG)
                        .show()
                return false
            }
            if (teams.isEmpty()) return false

            TeamSharer(activity, teams)
            logShareTeamsEvent(teams)
            return true
        }
    }
}

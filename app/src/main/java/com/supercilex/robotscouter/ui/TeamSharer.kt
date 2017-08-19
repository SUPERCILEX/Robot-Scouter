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
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.AsyncTaskExecutor
import com.supercilex.robotscouter.util.SINGLE_ITEM
import com.supercilex.robotscouter.util.TEAMS_LINK_BASE
import com.supercilex.robotscouter.util.data.CachingSharer
import com.supercilex.robotscouter.util.data.model.TeamCache
import com.supercilex.robotscouter.util.data.model.linkKeyNumberPair
import com.supercilex.robotscouter.util.isOffline
import com.supercilex.robotscouter.util.logShareTeamsEvent

class TeamSharer private constructor(private val activity: FragmentActivity,
                                     @Size(min = 1) teams: List<Team>) : CachingSharer(activity) {
    private val cache = Cache(teams)
    private val safeMessage: String get() {
        val fullMessage = cache.shareMessage
        return if (fullMessage.length >= MAX_MESSAGE_LENGTH) {
            activity.resources.getQuantityString(
                    R.plurals.share_team_message,
                    SINGLE_ITEM,
                    cache.teams[0].toString() + " and more")
        } else {
            fullMessage
        }
    }

    init {
        loadFile(FILE_NAME).continueWith(AsyncTaskExecutor, Continuation<String, Intent> {
            val deepLinkBuilder = StringBuilder("$TEAMS_LINK_BASE?")
            for (team in cache.teams) {
                deepLinkBuilder.append(team.linkKeyNumberPair)
            }

            getInvitationIntent(
                    deepLinkBuilder.toString(),
                    it.result.format(cache.shareCta, cache.teams[0].media))
        }).addOnSuccessListener {
            activity.startActivityForResult(it, RC_SHARE)
        }.addOnFailureListener {
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

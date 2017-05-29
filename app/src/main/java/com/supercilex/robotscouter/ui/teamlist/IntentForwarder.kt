package com.supercilex.robotscouter.ui.teamlist

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.FragmentActivity
import android.view.View
import com.firebase.ui.auth.util.GoogleApiHelper
import com.google.android.gms.appinvite.AppInvite
import com.google.android.gms.appinvite.AppInviteInvitationResult
import com.google.android.gms.appinvite.AppInviteReferral
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResultCallback
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.data.util.TeamHelper
import com.supercilex.robotscouter.ui.scout.ScoutListFragmentBase
import com.supercilex.robotscouter.util.SINGLE_ITEM
import com.supercilex.robotscouter.util.TEAM_QUERY_KEY

class IntentForwarder(private val activity: FragmentActivity) : ResultCallback<AppInviteInvitationResult> {
    init {
        AppInvite.AppInviteApi
                .getInvitation(
                        GoogleApiClient.Builder(activity)
                                .enableAutoManage(activity, GoogleApiHelper.getSafeAutoManageId(), null)
                                .addApi(AppInvite.API)
                                .build(),
                        null,
                        false)
                .setResultCallback(this)
    }

    override fun onResult(result: AppInviteInvitationResult) {
        if (result.status.isSuccess) { // Received invite from Firebase dynamic links
            val teams = getTeams(Uri.parse(AppInviteReferral.getDeepLink(result.invitationIntent)))
            for (team in teams) {
                val number = team.numberAsLong
                TeamHelper.getIndicesRef().child(team.key).setValue(number, number)
            }

            if (teams.size == SINGLE_ITEM) {
                launchTeam(teams[0])
            } else {
                Snackbar.make(activity.findViewById<View>(R.id.root),
                        R.string.teams_imported,
                        Snackbar.LENGTH_LONG)
                        .show()
            }
        } else { // Received normal intent
            val intent = activity.intent

            val extras: Bundle? = intent.extras
            if (extras != null) {
                if (extras.containsKey(DONATE)) {
                    DonateDialog.show(activity.supportFragmentManager)
                } else if (extras.containsKey(UPDATE)) {
                    UpdateDialog.showStoreListing(activity)
                }
            }

            val deepLink: Uri? = intent.data
            if (deepLink != null) {
                if (deepLink.getQueryParameter(TEAM_QUERY_KEY) != null) { // NOPMD https://github.com/pmd/pmd/issues/278
                    launchTeam(getTeams(deepLink)[0])
                } else if (deepLink.toString() == ADD_SCOUT_INTENT) {
                    NewTeamDialog.show(activity.supportFragmentManager)
                }
            }
        }

        // Consume intent
        activity.intent = Intent()
    }

    private fun getTeams(deepLink: Uri): List<Team> = deepLink.getQueryParameters(TEAM_QUERY_KEY).map {
        // Format: key:2521
        val teamPairSplit: Array<String> =
                it.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val teamKey = teamPairSplit[0]
        val teamNumber = teamPairSplit[1]

        Team.Builder(teamNumber).setKey(teamKey).build()
    }

    private fun launchTeam(team: Team) = (activity as TeamSelectionListener).onTeamSelected(
            ScoutListFragmentBase.getBundle(team, false, null))

    companion object {
        private const val ADD_SCOUT_INTENT = "add_scout"

        private const val DONATE = "donate"
        private const val UPDATE = "update"
    }
}

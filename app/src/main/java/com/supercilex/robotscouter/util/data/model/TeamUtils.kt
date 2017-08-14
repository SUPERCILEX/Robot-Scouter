package com.supercilex.robotscouter.util.data.model

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import com.google.android.gms.tasks.Task
import com.google.firebase.appindexing.Action
import com.google.firebase.appindexing.FirebaseAppIndex
import com.google.firebase.appindexing.FirebaseUserActions
import com.google.firebase.appindexing.Indexable
import com.google.firebase.appindexing.builders.Actions
import com.google.firebase.appindexing.builders.Indexables
import com.google.firebase.database.DatabaseReference
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.supercilex.robotscouter.data.client.startDownloadTeamDataJob
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.FIREBASE_TEAMS
import com.supercilex.robotscouter.util.FIREBASE_TEAM_INDICES
import com.supercilex.robotscouter.util.FIREBASE_TEMPLATE_KEY
import com.supercilex.robotscouter.util.FIREBASE_TIMESTAMP
import com.supercilex.robotscouter.util.KEY_QUERY
import com.supercilex.robotscouter.util.SINGLE_ITEM
import com.supercilex.robotscouter.util.TEAMS_LINK_BASE
import com.supercilex.robotscouter.util.fetchAndActivate
import com.supercilex.robotscouter.util.launchUrl
import com.supercilex.robotscouter.util.uid
import java.util.ArrayList
import java.util.Calendar
import java.util.Collections
import java.util.concurrent.TimeUnit

private const val TEAM_KEY = "com.supercilex.robotscouter.data.util.Team"
private const val TEAMS_KEY = "com.supercilex.robotscouter.data.util.Teams"
private const val FRESHNESS_DAYS = "team_freshness"

val teamIndicesRef: DatabaseReference get() = FIREBASE_TEAM_INDICES.child(uid!!)

val Team.ref: DatabaseReference get() = FIREBASE_TEAMS.child(key)

fun parseTeam(args: Bundle): Team = args.getParcelable(TEAM_KEY)

fun parseTeamList(intent: Intent): List<Team> = intent.getParcelableArrayListExtra(TEAMS_KEY)

fun parseTeamList(args: Bundle): List<Team> = args.getParcelableArrayList(TEAMS_KEY)

fun teamsToIntent(teams: List<Team>): Intent = Intent().putExtra(TEAMS_KEY, ArrayList(teams))

fun teamsToBundle(teams: List<Team>) =
        Bundle().apply { putParcelableArrayList(TEAMS_KEY, ArrayList(teams)) }

fun getTeamNames(teams: List<Team>): String {
    val sortedTeams = ArrayList(teams)
    Collections.sort(sortedTeams)

    return when {
        sortedTeams.size == SINGLE_ITEM -> sortedTeams[0].toString()
        sortedTeams.size == 2 -> "${sortedTeams[0]} and ${sortedTeams[1]}"
        else -> {
            val teamsMaxedOut = sortedTeams.size > 10
            val size = if (teamsMaxedOut) 10 else sortedTeams.size

            val names = StringBuilder(4 * size)
            for (i in 0 until size) {
                names.append(sortedTeams[i].number)
                if (i < size - 1) names.append(", ")
                if (i == size - 2 && !teamsMaxedOut) names.append("and ")
            }

            if (teamsMaxedOut) names.append(" and more")

            names.toString()
        }
    }
}

fun Team.toBundle() = Bundle().apply { putParcelable(TEAM_KEY, this@toBundle) }

fun Team.addTeam() {
    val index = teamIndicesRef.push()
    key = index.key
    val number = numberAsLong
    index.setValue(number, number)

    forceUpdateTeam()
    forceRefresh()

    FirebaseUserActions.getInstance().end(
            Action.Builder(Action.Builder.ADD_ACTION)
                    .setObject(toString(), deepLink)
                    .setActionStatus(Action.Builder.STATUS_TYPE_COMPLETED)
                    .build())
}

fun Team.updateTeam(newTeam: Team) {
    checkForMatchingTeamDetails(this, newTeam)
    if (this == newTeam) {
        ref.child(FIREBASE_TIMESTAMP).setValue(getCurrentTimestamp())
        return
    }

    if (!hasCustomName) name = newTeam.name
    if (!hasCustomMedia) {
        media = newTeam.media
        mediaYear = newTeam.mediaYear
    }
    if (!hasCustomWebsite) website = newTeam.website
    forceUpdateTeam()
}

private fun checkForMatchingTeamDetails(team: Team, newTeam: Team) {
    if (TextUtils.equals(team.name, newTeam.name)) team.hasCustomName = false
    if (TextUtils.equals(team.media, newTeam.media)) team.hasCustomMedia = false
    if (TextUtils.equals(team.website, newTeam.website)) team.hasCustomWebsite = false

}

fun Team.updateTemplateKey(key: String) {
    if (key == templateKey) return

    templateKey = key
    ref.child(FIREBASE_TEMPLATE_KEY).setValue(templateKey)
}

fun Team.updateMedia(newTeam: Team) {
    media = newTeam.media
    shouldUploadMediaToTba = false
    forceUpdateTeam()
}

fun Team.forceUpdateTeam() {
    ref.setValue(this)
    FirebaseAppIndex.getInstance().update(indexable)
}

fun Team.forceRefresh(): Task<Void?> = ref.child(FIREBASE_TIMESTAMP).removeValue()

fun Team.copyMediaInfo(newTeam: Team) {
    media = newTeam.media
    shouldUploadMediaToTba = newTeam.shouldUploadMediaToTba
    mediaYear = Calendar.getInstance().get(Calendar.YEAR)
}

fun Team.deleteTeam() {
    deleteAllScouts().addOnSuccessListener {
        ref.removeValue()
        teamIndicesRef.child(key).removeValue()
        FirebaseAppIndex.getInstance().remove(deepLink)
    }
}

fun Team.fetchLatestData(context: Context) {
    val appContext = context.applicationContext
    fetchAndActivate().addOnSuccessListener {
        val differenceDays = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - timestamp)
        val freshness = FirebaseRemoteConfig.getInstance().getDouble(FRESHNESS_DAYS)

        if (differenceDays >= freshness) startDownloadTeamDataJob(appContext, this)
    }
}

val Team.isOutdatedMedia: Boolean
    get() = mediaYear < Calendar.getInstance().get(Calendar.YEAR) || TextUtils.isEmpty(media)

fun Team.visitTbaWebsite(context: Context) =
        launchUrl(context, Uri.parse("https://www.thebluealliance.com/team/$number"))

fun Team.visitTeamWebsite(context: Context) = launchUrl(context, Uri.parse(website))

val Team.indexable: Indexable get() = Indexables.digitalDocumentBuilder()
        .setUrl(deepLink)
        .setName(toString())
        .apply { setImage(media ?: return@apply) }
        .setMetadata(Indexable.Metadata.Builder().setWorksOffline(true))
        .build()

private val Team.deepLink: String get() = "$TEAMS_LINK_BASE?$linkKeyNumberPair"

val Team.linkKeyNumberPair: String get() = "&$KEY_QUERY=$key:$number"

val Team.viewAction: Action get() = Actions.newView(toString(), deepLink)

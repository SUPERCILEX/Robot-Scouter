package com.supercilex.robotscouter.util.data.model

import android.content.Context
import android.net.Uri
import android.text.TextUtils
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.appindexing.Action
import com.google.firebase.appindexing.FirebaseAppIndex
import com.google.firebase.appindexing.FirebaseUserActions
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.supercilex.robotscouter.data.client.startDownloadTeamDataJob
import com.supercilex.robotscouter.data.model.Scout
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.FIRESTORE_OWNERS
import com.supercilex.robotscouter.util.FIRESTORE_POSITION
import com.supercilex.robotscouter.util.FIRESTORE_TEMPLATE_ID
import com.supercilex.robotscouter.util.FIRESTORE_TIMESTAMP
import com.supercilex.robotscouter.util.async
import com.supercilex.robotscouter.util.data.deepLink
import com.supercilex.robotscouter.util.data.indexable
import com.supercilex.robotscouter.util.data.metricParser
import com.supercilex.robotscouter.util.data.scoutParser
import com.supercilex.robotscouter.util.fetchAndActivate
import com.supercilex.robotscouter.util.isSingleton
import com.supercilex.robotscouter.util.launchUrl
import com.supercilex.robotscouter.util.logFailures
import com.supercilex.robotscouter.util.teams
import com.supercilex.robotscouter.util.uid
import java.util.ArrayList
import java.util.Calendar
import java.util.Collections
import java.util.Date
import java.util.concurrent.TimeUnit

private const val FRESHNESS_DAYS = "team_freshness"

val teamsQuery get() = "$FIRESTORE_OWNERS.${uid!!}".let {
    teams.whereGreaterThanOrEqualTo(it, 0).orderBy(it)
}

val Team.ref: DocumentReference get() = teams.document(id)

fun List<Team>.getNames(): String {
    val sortedTeams = ArrayList(this)
    Collections.sort(sortedTeams)

    return when {
        sortedTeams.isSingleton -> sortedTeams[0].toString()
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

fun Team.add() {
    id = teams.document().id
    forceUpdate()
    forceRefresh()

    FirebaseUserActions.getInstance().end(
            Action.Builder(Action.Builder.ADD_ACTION)
                    .setObject(toString(), deepLink)
                    .setActionStatus(Action.Builder.STATUS_TYPE_COMPLETED)
                    .build())
}

fun Team.update(newTeam: Team) {
    checkForMatchingTeamDetails(this, newTeam)
    if (this == newTeam) {
        ref.update(FIRESTORE_TIMESTAMP, getCurrentTimestamp())
        return
    }

    if (!hasCustomName) name = newTeam.name
    if (!hasCustomMedia) {
        media = newTeam.media
        mediaYear = newTeam.mediaYear
    }
    if (!hasCustomWebsite) website = newTeam.website
    forceUpdate()
}

private fun checkForMatchingTeamDetails(team: Team, newTeam: Team) {
    if (TextUtils.equals(team.name, newTeam.name)) team.hasCustomName = false
    if (TextUtils.equals(team.media, newTeam.media)) team.hasCustomMedia = false
    if (TextUtils.equals(team.website, newTeam.website)) team.hasCustomWebsite = false

}

fun Team.updateTemplateId(id: String) {
    if (id == templateId) return

    templateId = id
    ref.update(FIRESTORE_TEMPLATE_ID, templateId)
}

fun Team.updateMedia(newTeam: Team) {
    media = newTeam.media
    shouldUploadMediaToTba = false
    forceUpdate()
}

fun Team.forceUpdate() {
    ref.set(this)
    FirebaseAppIndex.getInstance().update(indexable)
}

fun Team.forceRefresh(): Task<Void?> = ref.update(FIRESTORE_TIMESTAMP, Date(0))

fun Team.copyMediaInfo(newTeam: Team) {
    media = newTeam.media
    shouldUploadMediaToTba = newTeam.shouldUploadMediaToTba
    mediaYear = Calendar.getInstance().get(Calendar.YEAR)
}

fun Team.delete() {
    deleteAllScouts().addOnSuccessListener {
        ref.delete()
        FirebaseAppIndex.getInstance().remove(deepLink)
    }.logFailures()
}

fun Team.fetchLatestData() {
    fetchAndActivate().addOnSuccessListener {
        val differenceDays = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - timestamp.time)
        val freshness = FirebaseRemoteConfig.getInstance().getDouble(FRESHNESS_DAYS)

        if (differenceDays >= freshness) startDownloadTeamDataJob(this)
    }
}

fun Team.getScouts(): Task<List<Scout>> = async {
    val scouts = Tasks.await(getScoutRef().orderBy(FIRESTORE_TIMESTAMP).get())
            .map { scoutParser.parseSnapshot(it) }
    val metricTasks =
            scouts.map { getScoutMetricsRef(it.id).orderBy(FIRESTORE_POSITION).get() }
    Tasks.await(Tasks.whenAll(metricTasks))

    scouts.mapIndexed { index, scout ->
        scout.copy(metrics = metricTasks[index].result.map { metricParser.parseSnapshot(it) })
    }
}

val Team.isOutdatedMedia: Boolean
    get() = mediaYear < Calendar.getInstance().get(Calendar.YEAR) || TextUtils.isEmpty(media)

fun Team.visitTbaWebsite(context: Context) =
        launchUrl(context, Uri.parse("https://www.thebluealliance.com/team/$number"))

fun Team.visitTeamWebsite(context: Context) = launchUrl(context, Uri.parse(website))

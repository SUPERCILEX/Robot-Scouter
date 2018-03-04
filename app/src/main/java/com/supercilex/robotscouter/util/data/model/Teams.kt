package com.supercilex.robotscouter.util.data.model

import android.content.Context
import android.support.annotation.WorkerThread
import android.util.Patterns
import androidx.net.toUri
import com.google.firebase.appindexing.Action
import com.google.firebase.appindexing.FirebaseAppIndex
import com.google.firebase.appindexing.FirebaseUserActions
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.SetOptions
import com.supercilex.robotscouter.data.client.startDownloadDataJob
import com.supercilex.robotscouter.data.model.Scout
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.FIRESTORE_OWNERS
import com.supercilex.robotscouter.util.FIRESTORE_POSITION
import com.supercilex.robotscouter.util.FIRESTORE_TEMPLATE_ID
import com.supercilex.robotscouter.util.FIRESTORE_TIMESTAMP
import com.supercilex.robotscouter.util.await
import com.supercilex.robotscouter.util.data.QueuedDeletion
import com.supercilex.robotscouter.util.data.deepLink
import com.supercilex.robotscouter.util.data.firestoreBatch
import com.supercilex.robotscouter.util.data.getInBatches
import com.supercilex.robotscouter.util.data.metricParser
import com.supercilex.robotscouter.util.data.scoutParser
import com.supercilex.robotscouter.util.fetchAndActivate
import com.supercilex.robotscouter.util.isSingleton
import com.supercilex.robotscouter.util.launchUrl
import com.supercilex.robotscouter.util.log
import com.supercilex.robotscouter.util.logAdd
import com.supercilex.robotscouter.util.logFailures
import com.supercilex.robotscouter.util.second
import com.supercilex.robotscouter.util.teamFreshnessDays
import com.supercilex.robotscouter.util.teams
import com.supercilex.robotscouter.util.uid
import kotlinx.coroutines.experimental.async
import java.io.File
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.sign

val teamsQuery
    get() = "$FIRESTORE_OWNERS.${uid!!}".let {
        teams.whereGreaterThanOrEqualTo(it, 0).orderBy(it)
    }

val Team.ref: DocumentReference get() = teams.document(id)

val Team.isStale: Boolean
    get() = TimeUnit.MILLISECONDS.toDays(
            System.currentTimeMillis() - timestamp.time
    ) >= teamFreshnessDays

val Team.isOutdatedMedia: Boolean
    get() = mediaYear < Calendar.getInstance().get(Calendar.YEAR) || media.isNullOrBlank()

val Team.isTrashed: Boolean?
    get() {
        return owners[uid ?: return null]?.sign == -1
    }

fun Collection<Team>.getNames(): String {
    val sortedTeams = toMutableList()
    sortedTeams.sort()

    return when {
        sortedTeams.isSingleton -> sortedTeams.single().toString()
        sortedTeams.size == 2 -> "${sortedTeams.first()} and ${sortedTeams.second()}"
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
    forceUpdateAndRefresh()

    logAdd()
    FirebaseUserActions.getInstance().end(
            Action.Builder(Action.Builder.ADD_ACTION)
                    .setObject(toString(), deepLink)
                    .setActionStatus(Action.Builder.STATUS_TYPE_COMPLETED)
                    .build()
    ).logFailures()
}

fun Team.update(newTeam: Team) {
    if (this == newTeam) {
        ref.log().update(FIRESTORE_TIMESTAMP, getCurrentTimestamp()).logFailures()
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

fun Team.updateTemplateId(id: String) {
    if (id == templateId) return

    templateId = id
    ref.log().update(FIRESTORE_TEMPLATE_ID, templateId).logFailures()
}

fun Team.updateMedia(newTeam: Team) {
    media = newTeam.media
    shouldUploadMediaToTba = false
    forceUpdate()
}

fun Team.forceUpdate() {
    ref.log().set(this).logFailures()
}

fun Team.forceUpdateAndRefresh() {
    firestoreBatch {
        set(ref.log(), this@forceUpdateAndRefresh)
        update(ref.log(), FIRESTORE_TIMESTAMP, Date(0))
    }.logFailures()
}

fun Team.copyMediaInfo(newTeam: Team) {
    media = newTeam.media
    hasCustomMedia = newTeam.hasCustomMedia
    shouldUploadMediaToTba = newTeam.shouldUploadMediaToTba
    mediaYear = newTeam.mediaYear
}

suspend fun Team.trash() {
    FirebaseAppIndex.getInstance().remove(deepLink).logFailures()
    firestoreBatch {
        update(ref.log(), "$FIRESTORE_OWNERS.${uid!!}", if (number == 0L) {
            -1 // Fatal flaw in our trashing architecture: -0 isn't a thing.
        } else {
            -abs(number)
        })
        set(userDeletionQueue.log(), QueuedDeletion.Team(ref.id).data, SetOptions.merge())
    }.await()
}

fun Team.fetchLatestData() = async {
    fetchAndActivate()
    if (isStale) startDownloadDataJob()
}.logFailures()

suspend fun Team.getScouts(): List<Scout> {
    val scouts = getScoutsQuery().getInBatches().map { scoutParser.parseSnapshot(it) }
    val metricsForScouts = scouts.map {
        async { getScoutMetricsRef(it.id).orderBy(FIRESTORE_POSITION).getInBatches() }
    }.await()
    return scouts.mapIndexed { index, scout ->
        scout.copy(metrics = metricsForScouts[index].map { metricParser.parseSnapshot(it) })
    }
}

fun Team.launchTba(context: Context) =
        launchUrl(context, "http://www.thebluealliance.com/team/$number".toUri())

fun Team.launchWebsite(context: Context) = launchUrl(context, website!!.toUri())

@WorkerThread
fun CharSequence.isValidTeamUrl() = toString().formatAsTeamUrl().let {
    it == null || Patterns.WEB_URL.matcher(it).matches() || File(it).exists()
}

@WorkerThread
fun String.formatAsTeamUrl(): String? {
    if (File(this).exists()) return this

    val trimmedUrl = trim()
    if (trimmedUrl.isBlank()) return null
    return if (trimmedUrl.contains("http://") || trimmedUrl.contains("https://")) {
        trimmedUrl
    } else {
        "http://$trimmedUrl"
    }
}

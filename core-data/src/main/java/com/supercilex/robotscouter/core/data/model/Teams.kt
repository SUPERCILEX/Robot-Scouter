package com.supercilex.robotscouter.core.data.model

import android.support.annotation.WorkerThread
import android.util.Patterns
import com.google.android.gms.tasks.Task
import com.google.firebase.appindexing.Action
import com.google.firebase.appindexing.FirebaseAppIndex
import com.google.firebase.appindexing.FirebaseUserActions
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.SetOptions
import com.supercilex.robotscouter.common.FIRESTORE_OWNERS
import com.supercilex.robotscouter.common.FIRESTORE_POSITION
import com.supercilex.robotscouter.common.FIRESTORE_TEMPLATE_ID
import com.supercilex.robotscouter.common.FIRESTORE_TIMESTAMP
import com.supercilex.robotscouter.core.await
import com.supercilex.robotscouter.core.data.QueryGenerator
import com.supercilex.robotscouter.core.data.QueuedDeletion
import com.supercilex.robotscouter.core.data.client.startDownloadDataJob
import com.supercilex.robotscouter.core.data.deepLink
import com.supercilex.robotscouter.core.data.defaultTemplateId
import com.supercilex.robotscouter.core.data.fetchAndActivate
import com.supercilex.robotscouter.core.data.firestoreBatch
import com.supercilex.robotscouter.core.data.getInBatches
import com.supercilex.robotscouter.core.data.isSingleton
import com.supercilex.robotscouter.core.data.logAdd
import com.supercilex.robotscouter.core.data.metricParser
import com.supercilex.robotscouter.core.data.scoutParser
import com.supercilex.robotscouter.core.data.second
import com.supercilex.robotscouter.core.data.teamFreshnessDays
import com.supercilex.robotscouter.core.data.teamsRef
import com.supercilex.robotscouter.core.data.uid
import com.supercilex.robotscouter.core.data.user
import com.supercilex.robotscouter.core.logFailures
import com.supercilex.robotscouter.core.model.Scout
import com.supercilex.robotscouter.core.model.Team
import kotlinx.coroutines.experimental.async
import java.io.File
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.sign

val teamWithSafeDefaults: (number: Long, id: String) -> Team = { number, id ->
    Team().apply {
        this.id = id
        this.number = number
        owners = mapOf(uid!! to number)
        templateId = defaultTemplateId
    }
}

val teamsQueryGenerator: QueryGenerator = {
    "$FIRESTORE_OWNERS.${it.uid}".let {
        teamsRef.whereGreaterThanOrEqualTo(it, 0).orderBy(it)
    }
}
val teamsQuery get() = teamsQueryGenerator(user!!)

val Team.ref: DocumentReference get() = teamsRef.document(id)

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
    id = teamsRef.document().id
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
        val timestamp = getCurrentTimestamp()
        ref.update(FIRESTORE_TIMESTAMP, timestamp).logFailures(ref, timestamp)
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
    ref.update(FIRESTORE_TEMPLATE_ID, templateId).logFailures(ref, templateId)
}

fun Team.updateMedia(newTeam: Team) {
    media = newTeam.media
    shouldUploadMediaToTba = false
    forceUpdate()
}

fun Team.forceUpdate() {
    ref.set(this).logFailures(ref, this)
}

fun Team.forceUpdateAndRefresh() {
    firestoreBatch {
        set(ref, this@forceUpdateAndRefresh)
        update(ref, FIRESTORE_TIMESTAMP, Date(0))
    }.logFailures(ref, this)
}

fun Team.copyMediaInfo(newTeam: Team) {
    media = newTeam.media
    hasCustomMedia = newTeam.hasCustomMedia
    shouldUploadMediaToTba = newTeam.shouldUploadMediaToTba
    mediaYear = newTeam.mediaYear
}

fun Team.trash(): Task<Void?> {
    FirebaseAppIndex.getInstance().remove(deepLink).logFailures()
    return firestoreBatch {
        update(ref, "$FIRESTORE_OWNERS.${uid!!}", if (number == 0L) {
            -1 // Fatal flaw in our trashing architecture: -0 isn't a thing.
        } else {
            -abs(number)
        })
        set(userDeletionQueue, QueuedDeletion.Team(ref.id).data, SetOptions.merge())
    }.logFailures(ref, this)
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

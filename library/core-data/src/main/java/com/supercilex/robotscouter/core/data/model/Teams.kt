package com.supercilex.robotscouter.core.data.model

import android.util.Patterns
import androidx.annotation.WorkerThread
import com.firebase.ui.firestore.SnapshotParser
import com.google.firebase.appindexing.Action
import com.google.firebase.appindexing.FirebaseAppIndex
import com.google.firebase.appindexing.FirebaseUserActions
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.supercilex.robotscouter.common.FIRESTORE_NUMBER
import com.supercilex.robotscouter.common.FIRESTORE_OWNERS
import com.supercilex.robotscouter.common.FIRESTORE_POSITION
import com.supercilex.robotscouter.common.FIRESTORE_TEMPLATE_ID
import com.supercilex.robotscouter.common.FIRESTORE_TIMESTAMP
import com.supercilex.robotscouter.common.isSingleton
import com.supercilex.robotscouter.common.second
import com.supercilex.robotscouter.core.await
import com.supercilex.robotscouter.core.data.QueryGenerator
import com.supercilex.robotscouter.core.data.QueuedDeletion
import com.supercilex.robotscouter.core.data.client.startDownloadDataJob
import com.supercilex.robotscouter.core.data.deepLink
import com.supercilex.robotscouter.core.data.defaultTemplateId
import com.supercilex.robotscouter.core.data.firestoreBatch
import com.supercilex.robotscouter.core.data.getInBatches
import com.supercilex.robotscouter.core.data.logAdd
import com.supercilex.robotscouter.core.data.logFailures
import com.supercilex.robotscouter.core.data.share
import com.supercilex.robotscouter.core.data.teamDuplicatesRef
import com.supercilex.robotscouter.core.data.teamFreshnessDays
import com.supercilex.robotscouter.core.data.teamsRef
import com.supercilex.robotscouter.core.data.uid
import com.supercilex.robotscouter.core.data.user
import com.supercilex.robotscouter.core.logFailures
import com.supercilex.robotscouter.core.model.Scout
import com.supercilex.robotscouter.core.model.Team
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.awaitAll
import kotlinx.coroutines.experimental.currentScope
import java.io.File
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.sign

val teamParser = SnapshotParser { checkNotNull(it.toObject(Team::class.java)).apply { id = it.id } }

val teamWithSafeDefaults: (number: Long, id: String) -> Team = { number, id ->
    Team().apply {
        this.id = id
        this.number = number
        owners = mapOf(checkNotNull(uid) to number)
        templateId = defaultTemplateId
    }
}

internal val teamsQueryGenerator: QueryGenerator = {
    "$FIRESTORE_OWNERS.${it.uid}".let {
        teamsRef.whereGreaterThanOrEqualTo(it, 0).orderBy(it)
    }
}
val teamsQuery get() = teamsQueryGenerator(checkNotNull(user))

val Team.ref: DocumentReference get() = teamsRef.document(id)

val Team.isOutdatedMedia: Boolean
    get() = mediaYear < Calendar.getInstance().get(Calendar.YEAR) || media.isNullOrBlank()

internal val Team.isStale: Boolean
    get() = TimeUnit.MILLISECONDS.toDays(
            System.currentTimeMillis() - timestamp.time
    ) >= teamFreshnessDays

internal val Team.isTrashed: Boolean?
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

internal fun Team.add() {
    id = teamsRef.document().id
    forceUpdate(true)
    teamDuplicatesRef.document(checkNotNull(uid)).set(mapOf(id to number), SetOptions.merge())

    logAdd()
    FirebaseUserActions.getInstance().end(
            Action.Builder(Action.Builder.ADD_ACTION)
                    .setObject(toString(), deepLink)
                    .setActionStatus(Action.Builder.STATUS_TYPE_COMPLETED)
                    .build()
    ).logFailures()
}

internal fun Team.update(newTeam: Team) {
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

internal fun Team.updateTemplateId(id: String) {
    if (id == templateId) return

    templateId = id
    ref.update(FIRESTORE_TEMPLATE_ID, templateId).logFailures(ref, templateId)
}

internal fun Team.updateMedia(newTeam: Team) {
    media = newTeam.media
    shouldUploadMediaToTba = false
    forceUpdate()
}

fun Team.forceUpdate(refresh: Boolean = false) {
    firestoreBatch {
        set(ref, this@forceUpdate)
        if (refresh) update(ref, FIRESTORE_TIMESTAMP, Date(0))
    }.logFailures(ref, this)
}

suspend fun List<DocumentReference>.shareTeams(
        block: Boolean = false
) = share(block) { token, ids ->
    QueuedDeletion.ShareToken.Team(token, ids)
}

fun Team.copyMediaInfo(newTeam: Team) {
    media = newTeam.media
    hasCustomMedia = newTeam.hasCustomMedia
    shouldUploadMediaToTba = newTeam.shouldUploadMediaToTba
    mediaYear = newTeam.mediaYear
}

fun Team.trash() {
    FirebaseAppIndex.getInstance().remove(deepLink).logFailures()
    firestoreBatch {
        val newNumber = if (number == 0L) {
            -1 // Fatal flaw in our trashing architecture: -0 isn't a thing.
        } else {
            -abs(number)
        }

        update(ref, "$FIRESTORE_OWNERS.${checkNotNull(uid)}", newNumber)
        set(teamDuplicatesRef.document(checkNotNull(uid)),
            mapOf(id to newNumber),
            SetOptions.merge())
        set(userDeletionQueue, QueuedDeletion.Team(ref.id).data, SetOptions.merge())
    }.logFailures(ref, this)
}

fun untrashTeam(id: String) {
    GlobalScope.async {
        val ref = teamsRef.document(id)
        val snapshot = ref.get().logFailures(ref).await()

        val newNumber = checkNotNull(snapshot.getLong(FIRESTORE_NUMBER))
        firestoreBatch {
            update(ref, "$FIRESTORE_OWNERS.${checkNotNull(uid)}", newNumber)
            update(teamDuplicatesRef.document(checkNotNull(uid)), id, newNumber)
            update(userDeletionQueue, id, FieldValue.delete())
        }.logFailures(id)
    }.logFailures()
}

internal fun Team.fetchLatestData() {
    if (isStale) startDownloadDataJob()
}

suspend fun Team.getScouts(): List<Scout> = currentScope {
    val scouts = getScoutsQuery().getInBatches().map { scoutParser.parseSnapshot(it) }
    val metricsForScouts = scouts.map {
        async { getScoutMetricsRef(it.id).orderBy(FIRESTORE_POSITION).getInBatches() }
    }.awaitAll()
    return scouts.mapIndexed { index, scout ->
        scout.copy(metrics = metricsForScouts[index].map { metricParser.parseSnapshot(it) })
    }
}

@WorkerThread
fun CharSequence.isValidTeamUri() = toString().formatAsTeamUri().let {
    it == null || Patterns.WEB_URL.matcher(it).matches() || File(it).exists()
}

@WorkerThread
fun String.formatAsTeamUri(): String? {
    val trimmedUrl = trim()
    if (trimmedUrl.isBlank()) return null

    if (File(this).exists()) return this

    return if (trimmedUrl.contains("http://") || trimmedUrl.contains("https://")) {
        trimmedUrl
    } else {
        "http://$trimmedUrl"
    }
}

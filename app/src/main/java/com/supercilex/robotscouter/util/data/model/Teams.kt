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
import com.google.firebase.firestore.DocumentSnapshot
import com.supercilex.robotscouter.data.client.startDownloadTeamDataJob
import com.supercilex.robotscouter.data.model.Scout
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.FIRESTORE_OWNERS
import com.supercilex.robotscouter.util.FIRESTORE_POSITION
import com.supercilex.robotscouter.util.FIRESTORE_TEMPLATE_ID
import com.supercilex.robotscouter.util.FIRESTORE_TIMESTAMP
import com.supercilex.robotscouter.util.data.deepLink
import com.supercilex.robotscouter.util.data.getInBatches
import com.supercilex.robotscouter.util.data.metricParser
import com.supercilex.robotscouter.util.data.scoutParser
import com.supercilex.robotscouter.util.doAsync
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

val Team.isOutdatedMedia: Boolean
    get() = mediaYear < Calendar.getInstance().get(Calendar.YEAR) || TextUtils.isEmpty(media)

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
    forceUpdate()
    forceRefresh()

    logAdd()
    FirebaseUserActions.getInstance().end(
            Action.Builder(Action.Builder.ADD_ACTION)
                    .setObject(toString(), deepLink)
                    .setActionStatus(Action.Builder.STATUS_TYPE_COMPLETED)
                    .build()
    ).logFailures()
}

fun Team.update(newTeam: Team) {
    checkForMatchingTeamDetails(this, newTeam)
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

private fun checkForMatchingTeamDetails(team: Team, newTeam: Team) {
    if (TextUtils.equals(team.name, newTeam.name)) team.hasCustomName = false
    if (TextUtils.equals(team.media, newTeam.media)) team.hasCustomMedia = false
    if (TextUtils.equals(team.website, newTeam.website)) team.hasCustomWebsite = false
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

fun Team.forceRefresh(): Task<Void?> = ref.log().update(FIRESTORE_TIMESTAMP, Date(0)).logFailures()

fun Team.copyMediaInfo(newTeam: Team) {
    media = newTeam.media
    shouldUploadMediaToTba = newTeam.shouldUploadMediaToTba
    mediaYear = Calendar.getInstance().get(Calendar.YEAR)
}

fun Team.trash() {
    ref.log().update("$FIRESTORE_OWNERS.${uid!!}", if (number == 0L) {
        -1 // Fatal flaw in our trashing architecture: -0 isn't a thing.
    } else {
        -abs(number)
    }).logFailures()
    FirebaseAppIndex.getInstance().remove(deepLink).logFailures()
}

fun Team.fetchLatestData() = async {
    fetchAndActivate()
    val differenceDays = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - timestamp.time)
    if (differenceDays >= teamFreshnessDays) startDownloadTeamDataJob(this@fetchLatestData)
}.logFailures()

fun Team.getScouts(): Task<List<Scout>> = doAsync {
    val scouts = Tasks.await(getScoutsQuery().getInBatches()).map {
        scoutParser.parseSnapshot(it)
    }
    val metricTasks = Tasks.await(Tasks.whenAllSuccess<List<DocumentSnapshot>>(scouts.map {
        getScoutMetricsRef(it.id).orderBy(FIRESTORE_POSITION).getInBatches()
    }))

    scouts.mapIndexed { index, scout ->
        scout.copy(metrics = metricTasks[index].map { metricParser.parseSnapshot(it) })
    }
}

fun Team.visitTbaWebsite(context: Context) =
        launchUrl(context, Uri.parse("https://www.thebluealliance.com/team/$number"))

fun Team.visitTeamWebsite(context: Context) = launchUrl(context, Uri.parse(website))

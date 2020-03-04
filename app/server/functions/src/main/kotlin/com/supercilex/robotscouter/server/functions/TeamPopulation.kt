package com.supercilex.robotscouter.server.functions

import com.supercilex.robotscouter.common.FIRESTORE_MEDIA
import com.supercilex.robotscouter.common.FIRESTORE_NAME
import com.supercilex.robotscouter.common.FIRESTORE_NUMBER
import com.supercilex.robotscouter.common.FIRESTORE_TIMESTAMP
import com.supercilex.robotscouter.common.FIRESTORE_WEBSITE
import com.supercilex.robotscouter.server.utils.FIRESTORE_HAS_CUSTOM_MEDIA
import com.supercilex.robotscouter.server.utils.FIRESTORE_HAS_CUSTOM_NAME
import com.supercilex.robotscouter.server.utils.FIRESTORE_HAS_CUSTOM_WEBSITE
import com.supercilex.robotscouter.server.utils.FIRESTORE_MEDIA_YEAR
import com.supercilex.robotscouter.server.utils.fetch
import com.supercilex.robotscouter.server.utils.moment
import com.supercilex.robotscouter.server.utils.types.Change
import com.supercilex.robotscouter.server.utils.types.DeltaDocumentSnapshot
import com.supercilex.robotscouter.server.utils.types.DocumentReference
import com.supercilex.robotscouter.server.utils.types.SetOptions
import com.supercilex.robotscouter.server.utils.types.Timestamp
import com.supercilex.robotscouter.server.utils.types.Timestamps
import com.supercilex.robotscouter.server.utils.types.functions
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asPromise
import kotlinx.coroutines.async
import kotlinx.coroutines.await
import kotlin.js.Date
import kotlin.js.Json
import kotlin.js.Promise
import kotlin.js.json

private const val TBA_API_BASE = "https://www.thebluealliance.com/api/v3"
private const val MAX_TBA_MEDIA_HISTORY = 2000

private val legalErrorMarker: Any = json()
private val tbaApiKey by lazy {
    functions.config().creds.tba_api_key.unsafeCast<String>()
}
private val refreshRateDays by lazy {
    functions.config().config.team_refresh_rate_days.unsafeCast<String>().toInt()
}

private val cache by lazy {
    @Suppress("UNUSED_VARIABLE") // Used in JS
    val options = json(
            "max" to 200000, // Assume each object is ~1KB and limit memory to 200MBs
            "maxAge" to 86400000 // 1 day
    )

    js("var LRU = require('lru-cache'); new LRU(options)")
}

fun populateTeam(event: Change<DeltaDocumentSnapshot>): Promise<*>? {
    val snapshot = event.after
    console.log("Populating team '${snapshot.id}'.")

    if (!snapshot.exists) return null

    val lastUpdate =
            snapshot.get(FIRESTORE_TIMESTAMP).unsafeCast<Timestamp?>()?.toDate() ?: Date(0)
    val timeSinceLastUpdate = moment().diff(lastUpdate, "days") as Int
    if (timeSinceLastUpdate < refreshRateDays) {
        console.log("Ignoring young team, last updated $timeSinceLastUpdate days ago.")
        return null
    }

    return GlobalScope.async {
        snapshot.ref.populateTeam(snapshot.data())
    }.asPromise()
}

private suspend fun DocumentReference.populateTeam(oldTeam: Json) {
    val updatableProperties = getUpdatableProperties(oldTeam)
    if (updatableProperties.isEmpty()) {
        console.log("No properties to update, updating timestamp.")
        set(json(FIRESTORE_TIMESTAMP to Timestamps.now()), SetOptions.merge).await()
        return
    }

    populateTeam(oldTeam, updatableProperties)
}

private suspend fun DocumentReference.populateTeam(
        oldTeam: Json,
        updatableProperties: Set<String>
) {
    val number = oldTeam[FIRESTORE_NUMBER].unsafeCast<Int>()
    val rawTeam = fetchTeam(number)
    val newTeam = rawTeam ?: json()

    val update = json(FIRESTORE_TIMESTAMP to Timestamps.now())
    l@ for (property in updatableProperties) {
        @Suppress("USELESS_ELVIS_RIGHT_IS_NULL")
        update[property] = when (property) {
            FIRESTORE_NAME -> newTeam["nickname"]
            FIRESTORE_WEBSITE -> newTeam["website"]
            FIRESTORE_MEDIA -> continue@l
            else -> error("Unknown property: $property")
        } ?: null
    }

    if (FIRESTORE_MEDIA in updatableProperties) {
        val rawMedia = if (rawTeam == null) null else fetchMedia(number)
        val (media, year) = rawMedia ?: null to 0

        update[FIRESTORE_MEDIA] = media
        update[FIRESTORE_MEDIA_YEAR] = year
    }

    console.log("Updating team with new details: '${JSON.stringify(update)}'.")
    set(update, SetOptions.merge).await()
}

private suspend fun fetchTeam(number: Int): Json? {
    val cacheKey = "team/$number"
    val url = "$TBA_API_BASE/team/frc$number?X-TBA-Auth-Key=$tbaApiKey"
    return fetchWithCache(url, cacheKey, ::parseTeam)
}

private suspend fun fetchMedia(number: Int, year: Int = Date().getFullYear()): Pair<String, Int>? {
    val cacheKey = "team/$number/media/$year"
    val url = "$TBA_API_BASE/team/frc$number/media/$year?X-TBA-Auth-Key=$tbaApiKey"

    val media = fetchWithCache(url, cacheKey, ::parseMedia)
    return when {
        media != null -> media to year
        year > MAX_TBA_MEDIA_HISTORY -> fetchMedia(number, year - 1)
        else -> null
    }
}

private fun parseTeam(team: Json): Json {
    return json(
            "nickname" to team["nickname"],
            "website" to team["website"]
    )
}

private fun parseMedia(responses: Array<Json>): String? {
    val comparator = compareBy<Json> {
        if (it["preferred"] == true) 1 else 0
    }.thenBy {
        when (it["type"]) {
            "imgur" -> 0
            "instagram-image" -> -1
            "cdphotothread" -> -3
            "youtube" -> -4
            else -> Int.MIN_VALUE
        }
    }.reversed()

    val topPick = responses.filterNot {
        (it["direct_url"] as? String).isNullOrBlank()
    }.sortedWith(comparator).firstOrNull()

    return topPick?.let { it["direct_url"] } as String?
}

private suspend fun <IN, OUT> fetchWithCache(
        url: String,
        cacheKey: String,
        mapper: (IN) -> OUT
): OUT? {
    val cachedResult = cache.get(cacheKey)
    if (cachedResult != null) {
        return if (cachedResult === legalErrorMarker) null else cachedResult
    }

    val response = fetch(url).await()
    if (response.status == 404.toShort()) {
        cache.set(cacheKey, legalErrorMarker)
        return null
    }
    if (!response.ok) {
        console.error(response.statusText)
        return null
    }

    val freshResult = mapper(response.json().await().asDynamic())
    cache.set(cacheKey, freshResult ?: legalErrorMarker)
    return freshResult
}

private fun getUpdatableProperties(team: Json): Set<String> {
    val hasCustomName = team[FIRESTORE_HAS_CUSTOM_NAME].unsafeCast<Boolean?>() ?: false
    val hasCustomMedia = team[FIRESTORE_HAS_CUSTOM_MEDIA].unsafeCast<Boolean?>() ?: false
    val hasCustomWebsite = team[FIRESTORE_HAS_CUSTOM_WEBSITE].unsafeCast<Boolean?>() ?: false

    return listOfNotNull(
            FIRESTORE_NAME.takeUnless { hasCustomName },
            FIRESTORE_MEDIA.takeUnless { hasCustomMedia },
            FIRESTORE_WEBSITE.takeUnless { hasCustomWebsite }
    ).toSet()
}

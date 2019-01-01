package com.supercilex.robotscouter.core.data

import android.os.Bundle
import androidx.core.os.bundleOf
import com.crashlytics.android.Crashlytics
import com.firebase.ui.common.ChangeEventType
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.FirebaseAnalytics.Param.CONTENT_TYPE
import com.google.firebase.analytics.FirebaseAnalytics.Param.ITEM_ID
import com.google.firebase.analytics.FirebaseAnalytics.Param.ITEM_NAME
import com.google.firebase.analytics.FirebaseAnalytics.Param.SUCCESS
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.data.model.getNames
import com.supercilex.robotscouter.core.isInTestMode
import com.supercilex.robotscouter.core.logCrashLog
import com.supercilex.robotscouter.core.logFailures
import com.supercilex.robotscouter.core.model.Metric
import com.supercilex.robotscouter.core.model.Team
import com.supercilex.robotscouter.core.model.TemplateType
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt

private const val SCOUT_ID = "scout_id"
private const val TEMPLATE_ID = "template_id"

/**
 * See [the size limitations]
 * [https://firebase.google.com/docs/reference/android/com/google/firebase/analytics/FirebaseAnalytics.html#logEvent(java.lang.String, android.os.Bundle)].
 */
private const val MAX_VALUE_LENGTH = 100
/** Accounts for two `…` chars */
private const val SEGMENT_SIZE = 98
private const val SEGMENT = "…"

private val analytics: FirebaseAnalytics by lazy { FirebaseAnalytics.getInstance(RobotScouter) }

private val prefLogger = object : ChangeEventListenerBase {
    override fun onChildChanged(
            type: ChangeEventType,
            snapshot: DocumentSnapshot,
            newIndex: Int,
            oldIndex: Int
    ) {
        if (type != ChangeEventType.ADDED && type != ChangeEventType.CHANGED) return

        val id = snapshot.id
        when (val pref = prefs[newIndex]) {
            is Boolean -> Crashlytics.setBool(id, pref)
            is String -> Crashlytics.setString(id, pref)
            is Int -> Crashlytics.setInt(id, pref)
            is Long -> Crashlytics.setLong(id, pref)
            is Double -> Crashlytics.setDouble(id, pref)
            is Float -> Crashlytics.setFloat(id, pref)
        }
    }
}

fun initAnalytics() {
    analytics.setAnalyticsCollectionEnabled(!isInTestMode)
    prefs.addChangeEventListener(prefLogger)

    FirebaseAuth.getInstance().addAuthStateListener {
        val user = it.currentUser

        // Log uid to help debug db crashes
        logCrashLog("User id: ${user?.uid}")
        Crashlytics.setUserIdentifier(user?.uid)
        Crashlytics.setUserEmail(user?.email)
        Crashlytics.setUserName(user?.displayName)
        analytics.setUserId(user?.uid)
        analytics.setUserProperty(FirebaseAnalytics.UserProperty.SIGN_UP_METHOD, user?.providerId)
    }
}

internal fun logNotificationsEnabled(
        enabled: Boolean,
        channels: Map<String, Boolean>
) = analytics.logEvent(
        "notifications_enabled_status",
        bundleOf("enabled" to enabled, *channels.map { it.toPair() }.toTypedArray())
)

fun Team.logSelect() = analytics.logEvent(
        "select_team",
        bundleOf(
                ITEM_ID to id,
                ITEM_NAME to toSafeString(),
                CONTENT_TYPE to number
        )
)

internal fun Team.logAdd() = analytics.logEvent(
        "add_team",
        bundleOf(
                ITEM_ID to id,
                ITEM_NAME to toSafeString(),
                CONTENT_TYPE to number
        )
)

fun Team.logEditDetails() = analytics.logEvent(
        "edit_team_details",
        bundleOf(
                ITEM_ID to id,
                ITEM_NAME to toSafeString(),
                CONTENT_TYPE to number
        )
)

fun Team.logTakeMedia() = analytics.logEvent(
        "take_media",
        bundleOf(
                ITEM_ID to id,
                ITEM_NAME to toSafeString(),
                CONTENT_TYPE to number
        )
)

fun List<Team>.logShare() {
    GlobalScope.async {
        safeLog(this@logShare) { ids, name ->
            analytics.logEvent(
                    "share_teams",
                    bundleOf(ITEM_ID to ids, ITEM_NAME to name)
            )
        }
    }.logFailures()
}

fun List<Team>.logExport() {
    GlobalScope.async {
        safeLog(this@logExport) { ids, name ->
            analytics.logEvent(
                    "export_teams",
                    bundleOf(ITEM_ID to ids, ITEM_NAME to name)
            )
        }
    }.logFailures()
}

internal fun Team.logAddScout(scoutId: String, templateId: String) = analytics.logEvent(
        "add_scout",
        bundleOf(
                ITEM_ID to id,
                ITEM_NAME to toSafeString(),
                CONTENT_TYPE to number,
                SCOUT_ID to scoutId,
                TEMPLATE_ID to templateId
        )
)

fun Team.logSelectScout(scoutId: String, templateId: String) = analytics.logEvent(
        "select_scout",
        bundleOf(
                ITEM_ID to id,
                ITEM_NAME to toSafeString(),
                CONTENT_TYPE to number,
                SCOUT_ID to scoutId,
                TEMPLATE_ID to templateId
        )
)

internal fun logAddTemplate(templateId: String, type: TemplateType) = analytics.logEvent(
        "add_template",
        bundleOf(ITEM_ID to templateId, CONTENT_TYPE to type.id)
)

fun logSelectTemplate(templateId: String, templateName: String) = analytics.logEvent(
        "select_template",
        bundleOf(ITEM_ID to templateId, ITEM_NAME to templateName.toSafeString())
)

fun logShareTemplate(templateId: String, templateName: String) = analytics.logEvent(
        "share_template",
        bundleOf(ITEM_ID to templateId, ITEM_NAME to templateName.toSafeString())
)

internal fun Metric<*>.logAdd() = analytics.logEvent(
        "add_metric",
        bundleOf(
                ITEM_ID to ref.id,
                CONTENT_TYPE to id,
                ITEM_NAME to name.toSafeString()
        )
)

internal fun Metric<*>.logUpdate() = analytics.logEvent(
        "update_metric",
        bundleOf(
                ITEM_ID to ref.id,
                CONTENT_TYPE to id,
                ITEM_NAME to name.toSafeString()
        )
)

internal fun logUpdateDefaultTemplateId(id: String) = analytics.logEvent(
        "update_default_template_id",
        bundleOf(ITEM_ID to id)
)

fun logRatingDialogResponse(yes: Boolean) = analytics.logEvent(
        "rating_response",
        bundleOf(SUCCESS to yes)
)

fun logLoginEvent() = analytics.logEvent(FirebaseAnalytics.Event.LOGIN, Bundle.EMPTY)

private fun Any?.toSafeString() = toString().let {
    it.substring(0 until if (it.length >= MAX_VALUE_LENGTH) MAX_VALUE_LENGTH else it.length)
}

private fun safeLog(teams: List<Team>, log: (ids: String, name: String) -> Unit) {
    val originalIds = teams.map(Team::id).toString()
    val originalName = teams.getNames()

    if (originalIds.length >= MAX_VALUE_LENGTH || originalName.length >= MAX_VALUE_LENGTH) {
        val sanitizedIds = segment(originalIds)
        val sanitizedName = segment(originalName)

        if (sanitizedIds.size >= sanitizedName.size) {
            for ((index, idSegment) in sanitizedIds.withIndex()) {
                log(idSegment, if (index < sanitizedName.size) sanitizedName[index] else "")
            }
        } else {
            for ((index, nameSegment) in sanitizedName.withIndex()) {
                log(if (index < sanitizedIds.size) sanitizedIds[index] else "", nameSegment)
            }
        }
    } else {
        log(originalIds, originalName)
    }
}

private fun segment(long: String): List<String> {
    val nSegments = ceil(long.length / SEGMENT_SIZE.toFloat()).roundToInt() - 1
    return (0..nSegments).mapIndexed { index, value ->
        val isMiddle = index in 1 until nSegments

        val start = if (nSegments > 0 && (index == nSegments || isMiddle)) SEGMENT else ""
        val slice = long.slice(
                value * SEGMENT_SIZE until min(long.length, (value + 1) * SEGMENT_SIZE))
        val end = if (nSegments > 0 && (index == 0 || isMiddle)) SEGMENT else ""

        start + slice + end
    }
}

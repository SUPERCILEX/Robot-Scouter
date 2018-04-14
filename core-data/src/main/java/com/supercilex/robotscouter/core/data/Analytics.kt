package com.supercilex.robotscouter.core.data

import android.os.Bundle
import androidx.core.os.bundleOf
import com.crashlytics.android.Crashlytics
import com.firebase.ui.common.ChangeEventType
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.FirebaseAnalytics.Param.CONTENT_TYPE
import com.google.firebase.analytics.FirebaseAnalytics.Param.ITEM_ID
import com.google.firebase.analytics.FirebaseAnalytics.Param.ITEM_NAME
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.SetOptions
import com.supercilex.robotscouter.common.FIRESTORE_LAST_LOGIN
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.data.model.add
import com.supercilex.robotscouter.core.data.model.getNames
import com.supercilex.robotscouter.core.data.model.userRef
import com.supercilex.robotscouter.core.isInTestMode
import com.supercilex.robotscouter.core.logCrashLog
import com.supercilex.robotscouter.core.logFailures
import com.supercilex.robotscouter.core.model.Metric
import com.supercilex.robotscouter.core.model.Team
import com.supercilex.robotscouter.core.model.TemplateType
import com.supercilex.robotscouter.core.model.User
import kotlinx.coroutines.experimental.async
import java.util.Date
import java.util.concurrent.TimeUnit
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

private val updateLastLogin = object : Runnable {
    override fun run() {
        if (isSignedIn) {
            val lastLogin = mapOf(FIRESTORE_LAST_LOGIN to Date())
            userRef.set(lastLogin, SetOptions.merge()).logFailures(userRef, lastLogin)
        }

        mainHandler.removeCallbacks(this)
        mainHandler.postDelayed(this, TimeUnit.DAYS.toMillis(1))
    }
}

private val prefLogger = object : ChangeEventListenerBase {
    override fun onChildChanged(
            type: ChangeEventType,
            snapshot: DocumentSnapshot,
            newIndex: Int,
            oldIndex: Int
    ) {
        if (type != ChangeEventType.ADDED && type != ChangeEventType.CHANGED) return

        val id = snapshot.id
        val pref = prefs[newIndex]
        when (pref) {
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

        if (user != null) {
            User(
                    user.uid,
                    user.email.nullOrFull(),
                    user.phoneNumber.nullOrFull(),
                    user.displayName.nullOrFull(),
                    user.photoUrl
            ).add()

            updateLastLogin.run()
        }
    }
}

fun Team.logSelect() = analytics.logEvent(
        "select_team",
        bundleOf(
                ITEM_ID to id,
                ITEM_NAME to toSafeString(),
                CONTENT_TYPE to number
        )
)

fun Team.logAdd() = analytics.logEvent(
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
    async {
        safeLog(this@logShare) { ids, name ->
            analytics.logEvent(
                    "share_teams",
                    bundleOf(ITEM_ID to ids, ITEM_NAME to name)
            )
        }
    }.logFailures()
}

fun List<Team>.logExport() {
    async {
        safeLog(this@logExport) { ids, name ->
            analytics.logEvent(
                    "export_teams",
                    bundleOf(ITEM_ID to ids, ITEM_NAME to name)
            )
        }
    }.logFailures()
}

fun Team.logAddScout(scoutId: String, templateId: String) = analytics.logEvent(
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

fun logAddTemplate(templateId: String, type: TemplateType) = analytics.logEvent(
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

fun Metric<*>.logAdd() = analytics.logEvent(
        "add_metric",
        bundleOf(
                ITEM_ID to ref.id,
                CONTENT_TYPE to id,
                ITEM_NAME to name.toSafeString()
        )
)

fun Metric<*>.logUpdate() = analytics.logEvent(
        "update_metric",
        bundleOf(
                ITEM_ID to ref.id,
                CONTENT_TYPE to id,
                ITEM_NAME to name.toSafeString()
        )
)

fun logUpdateDefaultTemplateId(id: String) = analytics.logEvent(
        "update_default_template_id",
        bundleOf(ITEM_ID to id)
)

fun logLoginEvent() = analytics.logEvent(FirebaseAnalytics.Event.LOGIN, Bundle.EMPTY)

private fun Any?.toSafeString() = toString().let {
    it.substring(0 until if (it.length >= MAX_VALUE_LENGTH) MAX_VALUE_LENGTH else it.length)
}

private fun safeLog(teams: List<Team>, log: (ids: String, name: String) -> Unit) {
    val originalIds = teams.map { it.id }.toString()
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

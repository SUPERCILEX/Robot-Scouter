package com.supercilex.robotscouter.util

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import com.crashlytics.android.Crashlytics
import com.firebase.ui.common.ChangeEventType
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.Task
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.FirebaseAnalytics.Event
import com.google.firebase.analytics.FirebaseAnalytics.Param.CONTENT_TYPE
import com.google.firebase.analytics.FirebaseAnalytics.Param.ITEM_ID
import com.google.firebase.analytics.FirebaseAnalytics.Param.ITEM_NAME
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crash.FirebaseCrash
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.supercilex.robotscouter.BuildConfig
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.data.model.Metric
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.data.model.TemplateType
import com.supercilex.robotscouter.data.model.User
import com.supercilex.robotscouter.util.data.PrefObserver
import com.supercilex.robotscouter.util.data.PrefsLiveData
import com.supercilex.robotscouter.util.data.model.add
import com.supercilex.robotscouter.util.data.model.getNames
import com.supercilex.robotscouter.util.data.model.userRef
import kotlinx.coroutines.experimental.CompletionHandler
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.error
import java.lang.Exception
import java.util.Date
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

fun initAnalytics() {
    analytics.setAnalyticsCollectionEnabled(!isInTestMode)
    PrefLogger

    FirebaseAuth.getInstance().addAuthStateListener {
        fun String?.nullOrFull() = if (TextUtils.isEmpty(this)) null else this

        val user = it.currentUser

        // Log uid to help debug db crashes
        FirebaseCrash.log("User id: ${user?.uid}")
        Crashlytics.log("User id: ${user?.uid}")
        Crashlytics.setUserIdentifier(user?.uid)
        Crashlytics.setUserEmail(user?.email)
        Crashlytics.setUserName(user?.displayName)
        analytics.setUserId(user?.uid)
        analytics.setUserProperty(
                FirebaseAnalytics.UserProperty.SIGN_UP_METHOD, user?.providers.toString())

        if (user != null) {
            User(
                    user.uid,
                    user.email.nullOrFull(),
                    user.phoneNumber.nullOrFull(),
                    user.displayName.nullOrFull(),
                    user.photoUrl
            ).add()
        }
    }

    // Use an IdTokenListener which updates every hour or so to ensure the last login time gets
    // updated accurately. If we used an AuthStateListener, it would only update when the app is
    // restarted.
    FirebaseAuth.getInstance().addIdTokenListener {
        val available =
                GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(RobotScouter)
        "Play Services availability: $available".also {
            FirebaseCrash.log(it)
            Crashlytics.log(it)
        }

        if (uid != null) {
            userRef.log().set(mapOf(FIRESTORE_LAST_LOGIN to Date()), SetOptions.merge())
                    .logFailures()
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

fun List<Team>.logShare() = launch {
    safeLog(this@logShare) { ids, name ->
        analytics.logEvent(
                "share_teams",
                bundleOf(ITEM_ID to ids, ITEM_NAME to name)
        )
    }
}

fun List<Team>.logExport() = launch {
    safeLog(this@logExport) { ids, name ->
        analytics.logEvent(
                "export_teams",
                bundleOf(ITEM_ID to ids, ITEM_NAME to name)
        )
    }
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

fun logLoginEvent() = analytics.logEvent(Event.LOGIN, Bundle())

inline fun <T, reified E> Task<T>.logIgnorableFailures() = logFailures { it is E }

fun <T> Task<T>.logFailures(
        ignore: ((Exception) -> Boolean)? = null
): Task<T> {
    val trace = Thread.currentThread().stackTrace.filter {
        it.className.contains("supercilex")
    }.let {
        it.subList(1, it.size)
    }
    return addOnFailureListener {
        if (ignore?.invoke(it)?.not() != false) {
            logCrashLog("Crash source: $trace")
            CrashLogger.onFailure(it)
        }
    }
}

fun <T> Deferred<T>.logFailures(): Deferred<T> {
    invokeOnCompletion(CrashLogger)
    return this
}

fun DocumentReference.log(): DocumentReference {
    logDbUse(path)
    return this
}

fun Query.log(): Query {
    if (this is CollectionReference) return log()

    logDbUse("")
    return this
}

fun CollectionReference.log(): CollectionReference {
    logDbUse(path)
    return this
}

private fun logDbUse(path: String) {
    val trace = Thread.currentThread().stackTrace.filter {
        it.className.contains("supercilex")
    }.let {
        it.subList(2, it.size)
    }
    logCrashLog("Used reference '$path' at $trace")
}

private fun logCrashLog(message: String) {
    Crashlytics.log(message)
    FirebaseCrash.log(message)
    if (BuildConfig.DEBUG) Log.d("CrashLogs", message)
}

object CrashLogger : OnFailureListener, OnCompleteListener<Any>, CompletionHandler, AnkoLogger {
    override fun onFailure(e: Exception) {
        invoke(e)
    }

    override fun onComplete(task: Task<Any>) {
        invoke(task.exception)
    }

    override fun invoke(cause: Throwable?) {
        cause ?: return
        FirebaseCrash.report(cause)
        Crashlytics.logException(cause)
        if (BuildConfig.DEBUG || isInTestMode) {
            error("An unknown error occurred", cause)
        }
    }
}

private object PrefLogger : PrefObserver() {
    override fun onChildChanged(
            type: ChangeEventType,
            snapshot: DocumentSnapshot,
            newIndex: Int,
            oldIndex: Int
    ) {
        if (type != ChangeEventType.ADDED && type != ChangeEventType.CHANGED) return

        val id = snapshot.id
        val pref = PrefsLiveData.value!![newIndex]
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

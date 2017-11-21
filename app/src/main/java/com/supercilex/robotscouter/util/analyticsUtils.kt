package com.supercilex.robotscouter.util

import android.os.Bundle
import com.crashlytics.android.Crashlytics
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.Task
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.FirebaseAnalytics.Event
import com.google.firebase.analytics.FirebaseAnalytics.Param.CONTENT_TYPE
import com.google.firebase.analytics.FirebaseAnalytics.Param.ITEM_CATEGORY
import com.google.firebase.analytics.FirebaseAnalytics.Param.ITEM_ID
import com.google.firebase.analytics.FirebaseAnalytics.Param.ITEM_NAME
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crash.FirebaseCrash
import com.google.firebase.firestore.SetOptions
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.data.model.User
import com.supercilex.robotscouter.util.data.model.add
import com.supercilex.robotscouter.util.data.model.getNames
import com.supercilex.robotscouter.util.data.model.userRef
import io.reactivex.functions.Consumer
import io.reactivex.plugins.RxJavaPlugins
import org.jetbrains.anko.bundleOf
import java.lang.Exception
import java.util.Date
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt

private const val TEAM_ID = "team_id"
private const val SCOUT_ID = "scout_id"
private const val TEMPLATE_ID = "template_id"

private const val TEAM_CATEGORY = "team"
private const val TEAMS_CATEGORY = "teams"
private const val SCOUT_CATEGORY = "scout"
private const val TEMPLATE_CATEGORY = "template"

/**
 * See [the size limitations]
 * [https://firebase.google.com/docs/reference/android/com/google/firebase/analytics/FirebaseAnalytics.html#logEvent(java.lang.String, android.os.Bundle)].
 */
private const val MAX_VALUE_LENGTH = 100
/** Accounts for two `…` chars */
private const val SEGMENT_SIZE = 98
private const val SEGMENT = "…"

private val analytics: FirebaseAnalytics by lazy {
    FirebaseAnalytics.getInstance(RobotScouter.INSTANCE)
}

fun initAnalytics() {
    analytics.setAnalyticsCollectionEnabled(!isInTestMode)

    FirebaseAuth.getInstance().addAuthStateListener {
        val user = it.currentUser

        // Log uid to help debug db crashes
        FirebaseCrash.log("User id: ${user?.uid}")
        Crashlytics.setUserIdentifier(user?.uid)
        Crashlytics.setUserEmail(user?.email)
        Crashlytics.setUserName(user?.displayName)
        analytics.setUserId(user?.uid)
        analytics.setUserProperty(
                FirebaseAnalytics.UserProperty.SIGN_UP_METHOD, user?.providers.toString())

        if (user != null) {
            User(user.uid, user.email, user.phoneNumber, user.displayName, user.photoUrl).add()
        }
    }

    // Use an IdTokenListener which updates every hour or so to ensure the last login time gets
    // updated accurately. If we used an AuthStateListener, it would only update when the app is
    // restarted.
    FirebaseAuth.getInstance().addIdTokenListener {
        if (uid != null) {
            userRef.set(mapOf(FIRESTORE_LAST_LOGIN to Date()), SetOptions.merge())
        }
    }

    RxJavaPlugins.setErrorHandler(CrashLogger)
}

fun logSelectTeamEvent(team: Team) = analytics.logEvent(
        Event.SELECT_CONTENT,
        bundleOf(
                ITEM_ID to "select_team",
                CONTENT_TYPE to TEAM_CATEGORY,
                ITEM_NAME to team.number,
                TEAM_ID to team.id
        )
)

fun logEditTeamDetailsEvent(team: Team) = analytics.logEvent(
        Event.VIEW_ITEM,
        bundleOf(
                ITEM_ID to "edit_team_details",
                ITEM_NAME to team.number,
                ITEM_CATEGORY to TEAM_CATEGORY,
                TEAM_ID to team.id
        )
)

fun logShareTeamsEvent(teams: List<Team>) {
    async {
        safeLog(teams) { ids, name ->
            analytics.logEvent(
                    Event.SHARE,
                    bundleOf(
                            ITEM_ID to "share_team",
                            ITEM_NAME to name,
                            ITEM_CATEGORY to TEAMS_CATEGORY,
                            TEAM_ID to ids
                    )
            )
        }
    }.logFailures()
}

fun logAddScoutEvent(team: Team, scoutId: String, templateId: String) = analytics.logEvent(
        Event.VIEW_ITEM,
        bundleOf(
                ITEM_ID to "add_scout",
                ITEM_NAME to team.number,
                ITEM_CATEGORY to SCOUT_CATEGORY,
                TEAM_ID to team.id,
                SCOUT_ID to scoutId,
                TEMPLATE_ID to templateId
        )
)

fun logAddTemplateEvent(templateId: String) = analytics.logEvent(
        Event.VIEW_ITEM,
        bundleOf(
                ITEM_ID to "add_template",
                ITEM_NAME to "Template",
                ITEM_CATEGORY to TEMPLATE_CATEGORY,
                TEMPLATE_ID to templateId
        )
)

fun logViewTemplateEvent(templateId: String) = analytics.logEvent(
        Event.VIEW_ITEM,
        bundleOf(
                ITEM_ID to "view_template",
                ITEM_NAME to "Template",
                ITEM_CATEGORY to TEMPLATE_CATEGORY,
                TEMPLATE_ID to templateId
        )
)

fun logShareTemplateEvent(templateId: String) = analytics.logEvent(
        Event.SHARE,
        bundleOf(
                ITEM_ID to "share_template",
                ITEM_NAME to "Template",
                ITEM_CATEGORY to TEMPLATE_CATEGORY,
                TEAM_ID to templateId
        )
)

fun logExportTeamsEvent(teams: List<Team>) {
    async {
        safeLog(teams) { ids, name ->
            analytics.logEvent(
                    Event.VIEW_ITEM,
                    bundleOf(
                            ITEM_ID to "export_teams",
                            ITEM_NAME to name,
                            ITEM_CATEGORY to TEAMS_CATEGORY,
                            TEAM_ID to ids
                    )
            )
        }
    }.logFailures()
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

fun <T> Task<T>.logFailures(): Task<T> = addOnFailureListener(CrashLogger)

object CrashLogger : OnFailureListener, OnCompleteListener<Any>, Consumer<Throwable> {
    override fun onFailure(e: Exception) {
        accept(e)
    }

    override fun onComplete(task: Task<Any>) {
        accept(task.exception ?: return)
    }

    override fun accept(t: Throwable) {
        FirebaseCrash.report(t)
        Crashlytics.logException(t)
    }
}

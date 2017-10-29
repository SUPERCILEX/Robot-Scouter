package com.supercilex.robotscouter.util

import android.os.Bundle
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
import com.supercilex.robotscouter.util.data.model.getTeamNames
import com.supercilex.robotscouter.util.data.model.userRef
import org.jetbrains.anko.bundleOf
import java.lang.Exception
import java.util.Date

private const val TEAM_ID = "team_id"
private const val SCOUT_ID = "scout_id"
private const val TEMPLATE_ID = "template_id"

private const val TEAM_CATEGORY = "team"
private const val TEAMS_CATEGORY = "teams"
private const val SCOUT_CATEGORY = "scout"
private const val TEMPLATE_CATEGORY = "template"

private val analytics: FirebaseAnalytics by lazy {
    FirebaseAnalytics.getInstance(RobotScouter.INSTANCE)
}

fun initAnalytics() {
    analytics.setAnalyticsCollectionEnabled(!isInTestMode)

    FirebaseAuth.getInstance().addAuthStateListener {
        val user = it.currentUser

        // Log uid to help debug db crashes
        FirebaseCrash.log("User id: ${user?.uid}")
        analytics.setUserId(user?.uid)
        analytics.setUserProperty(
                FirebaseAnalytics.UserProperty.SIGN_UP_METHOD, user?.providers.toString())

        if (user != null) User(user.uid, user.email, user.displayName, user.photoUrl).add()
    }

    FirebaseAuth.getInstance().addIdTokenListener {
        if (uid != null) {
            userRef.set(mapOf(FIRESTORE_LAST_LOGIN to Date()), SetOptions.merge())
        }
    }
}

fun logSelectTeamEvent(team: Team) = analytics.logEvent(Event.SELECT_CONTENT, bundleOf(
        ITEM_ID to "select_team",
        CONTENT_TYPE to TEAM_CATEGORY,
        ITEM_NAME to team.number,
        TEAM_ID to team.id))

fun logEditTeamDetailsEvent(team: Team) = analytics.logEvent(Event.VIEW_ITEM, bundleOf(
        ITEM_ID to "edit_team_details",
        ITEM_NAME to team.number,
        ITEM_CATEGORY to TEAM_CATEGORY,
        TEAM_ID to team.id))

fun logShareTeamsEvent(teams: List<Team>) = analytics.logEvent(Event.SHARE, bundleOf(
        ITEM_ID to "share_team",
        ITEM_NAME to getTeamNames(teams),
        ITEM_CATEGORY to TEAMS_CATEGORY,
        TEAM_ID to teams.map { it.id }.toString()))

fun logExportTeamsEvent(teams: List<Team>) = analytics.logEvent(Event.VIEW_ITEM, bundleOf(
        ITEM_ID to "export_teams",
        ITEM_NAME to getTeamNames(teams),
        ITEM_CATEGORY to TEAMS_CATEGORY,
        TEAM_ID to teams.map { it.id }.toString()))

fun logAddScoutEvent(team: Team, scoutId: String, templateId: String) = analytics.logEvent(Event.VIEW_ITEM, bundleOf(
        ITEM_ID to "add_scout",
        ITEM_NAME to team.number,
        ITEM_CATEGORY to SCOUT_CATEGORY,
        TEAM_ID to team.id,
        SCOUT_ID to scoutId,
        TEMPLATE_ID to templateId))

fun logAddTemplateEvent(templateId: String) = analytics.logEvent(Event.VIEW_ITEM, bundleOf(
        ITEM_ID to "add_template",
        ITEM_NAME to "Template",
        ITEM_CATEGORY to TEMPLATE_CATEGORY,
        TEMPLATE_ID to templateId))

fun logViewTemplateEvent(templateId: String) = analytics.logEvent(Event.VIEW_ITEM, bundleOf(
        ITEM_ID to "view_template",
        ITEM_NAME to "Template",
        ITEM_CATEGORY to TEMPLATE_CATEGORY,
        TEMPLATE_ID to templateId))

fun logShareTemplateEvent(templateId: String) = analytics.logEvent(Event.SHARE, bundleOf(
        ITEM_ID to "share_template",
        ITEM_NAME to "Template",
        ITEM_CATEGORY to TEMPLATE_CATEGORY,
        TEAM_ID to templateId))

fun logLoginEvent() = analytics.logEvent(Event.LOGIN, Bundle())

fun <T> Task<T>.logFailures(): Task<T> = addOnFailureListener(CrashLogger)

object CrashLogger : OnFailureListener, OnCompleteListener<Any> {
    override fun onFailure(e: Exception) = FirebaseCrash.report(e)

    override fun onComplete(task: Task<Any>) {
        onFailure(task.exception ?: return)
    }
}

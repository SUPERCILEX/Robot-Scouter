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
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.data.model.getTeamNames
import java.lang.Exception

private const val TEAM_ID = "team_id"
private const val TEMPLATE_ID = "template_id"

private const val TEAM_CATEGORY = "team"
private const val TEAMS_CATEGORY = "teams"
private const val SCOUT_CATEGORY = "scout"
private const val TEMPLATE_CATEGORY = "template"

private val analytics: FirebaseAnalytics by lazy {
    FirebaseAnalytics.getInstance(RobotScouter.INSTANCE)
}

fun initAnalytics() {
    FirebaseAuth.getInstance().addAuthStateListener {
        // Log uid to help debug db crashes
        FirebaseCrash.log("User id: $uid")
        analytics.setUserId(uid)
        analytics.setUserProperty(
                FirebaseAnalytics.UserProperty.SIGN_UP_METHOD, user?.providers.toString())
    }
}

fun logSelectTeamEvent(team: Team) = analytics.logEvent(Event.SELECT_CONTENT, Bundle().apply {
    putString(ITEM_ID, "select_team")
    putString(CONTENT_TYPE, TEAM_CATEGORY)
    putString(ITEM_NAME, team.number)
    putString(TEAM_ID, team.id)
})

fun logEditTeamDetailsEvent(team: Team) = analytics.logEvent(Event.VIEW_ITEM, Bundle().apply {
    putString(ITEM_ID, "edit_team_details")
    putString(ITEM_NAME, team.number)
    putString(ITEM_CATEGORY, TEAM_CATEGORY)
    putString(TEAM_ID, team.id)
})

fun logShareTeamsEvent(teams: List<Team>) = analytics.logEvent(Event.SHARE, Bundle().apply {
    putString(ITEM_ID, "share_team")
    putString(ITEM_NAME, getTeamNames(teams))
    putString(ITEM_CATEGORY, TEAMS_CATEGORY)
    putString(TEAM_ID, teams.map { it.id }.toString())
})

fun logExportTeamsEvent(teams: List<Team>) = analytics.logEvent(Event.VIEW_ITEM, Bundle().apply {
    putString(ITEM_ID, "export_teams")
    putString(ITEM_NAME, getTeamNames(teams))
    putString(ITEM_CATEGORY, TEAMS_CATEGORY)
    putString(TEAM_ID, teams.map { it.id }.toString())
})

fun logAddScoutEvent(team: Team, templateId: String) = analytics.logEvent(Event.VIEW_ITEM, Bundle().apply {
    putString(ITEM_ID, "add_scout")
    putString(ITEM_NAME, team.number)
    putString(ITEM_CATEGORY, SCOUT_CATEGORY)
    putString(TEAM_ID, team.id)
    putString(TEMPLATE_ID, templateId)
})

fun logAddTemplateEvent(templateId: String) = analytics.logEvent(Event.VIEW_ITEM, Bundle().apply {
    putString(ITEM_ID, "add_template")
    putString(ITEM_NAME, "Template")
    putString(ITEM_CATEGORY, TEMPLATE_CATEGORY)
    putString(TEMPLATE_ID, templateId)
})

fun logViewTemplateEvent(templateId: String) = analytics.logEvent(Event.VIEW_ITEM, Bundle().apply {
    putString(ITEM_ID, "view_template")
    putString(ITEM_NAME, "Template")
    putString(ITEM_CATEGORY, TEMPLATE_CATEGORY)
    putString(TEMPLATE_ID, templateId)
})

fun logShareTemplateEvent(templateId: String) = analytics.logEvent(Event.SHARE, Bundle().apply {
    putString(ITEM_ID, "share_template")
    putString(ITEM_NAME, "Template")
    putString(ITEM_CATEGORY, TEMPLATE_CATEGORY)
    putString(TEAM_ID, templateId)
})

fun logLoginEvent() = analytics.logEvent(Event.LOGIN, Bundle())

object CrashLogger : OnFailureListener, OnCompleteListener<Any> {
    override fun onFailure(e: Exception) {
        FirebaseCrash.report(e)
    }

    override fun onComplete(task: Task<Any>) {
        onFailure(task.exception ?: return)
    }
}

package com.supercilex.robotscouter.util

import android.os.Bundle
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
    putString(TEAM_ID, team.key)
})

fun logEditTeamDetailsEvent(team: Team) = analytics.logEvent(Event.VIEW_ITEM, Bundle().apply {
    putString(ITEM_ID, "edit_team_details")
    putString(ITEM_NAME, team.number)
    putString(ITEM_CATEGORY, TEAM_CATEGORY)
    putString(TEAM_ID, team.key)
})

fun logShareTeamsEvent(teams: List<Team>) = analytics.logEvent(Event.SHARE, Bundle().apply {
    putString(ITEM_ID, "share_team")
    putString(ITEM_NAME, getTeamNames(teams))
    putString(ITEM_CATEGORY, TEAMS_CATEGORY)
    putString(TEAM_ID, teams.map { it.key }.toString())
})

fun logExportTeamsEvent(teams: List<Team>) = analytics.logEvent(Event.VIEW_ITEM, Bundle().apply {
    putString(ITEM_ID, "export_teams")
    putString(ITEM_NAME, getTeamNames(teams))
    putString(ITEM_CATEGORY, TEAMS_CATEGORY)
    putString(TEAM_ID, teams.map { it.key }.toString())
})

fun logAddScoutEvent(team: Team, templateKey: String) = analytics.logEvent(Event.VIEW_ITEM, Bundle().apply {
    putString(ITEM_ID, "add_scout")
    putString(ITEM_NAME, team.number)
    putString(ITEM_CATEGORY, SCOUT_CATEGORY)
    putString(TEAM_ID, team.key)
    putString(TEMPLATE_ID, templateKey)
})

fun logAddTemplateEvent(templateKey: String) = analytics.logEvent(Event.VIEW_ITEM, Bundle().apply {
    putString(ITEM_ID, "add_template")
    putString(ITEM_NAME, "Template")
    putString(ITEM_CATEGORY, TEMPLATE_CATEGORY)
    putString(TEMPLATE_ID, templateKey)
})

fun logViewTemplateEvent(templateKey: String) = analytics.logEvent(Event.VIEW_ITEM, Bundle().apply {
    putString(ITEM_ID, "view_template")
    putString(ITEM_NAME, "Template")
    putString(ITEM_CATEGORY, TEMPLATE_CATEGORY)
    putString(TEMPLATE_ID, templateKey)
})

fun logShareTemplateEvent(templateKey: String) = analytics.logEvent(Event.SHARE, Bundle().apply {
    putString(ITEM_ID, "share_template")
    putString(ITEM_NAME, "Template")
    putString(ITEM_CATEGORY, TEMPLATE_CATEGORY)
    putString(TEAM_ID, templateKey)
})

fun logLoginEvent() = analytics.logEvent(Event.LOGIN, Bundle())

package com.supercilex.robotscouter.util

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.FirebaseAnalytics.Event
import com.google.firebase.analytics.FirebaseAnalytics.Param.ITEM_CATEGORY
import com.google.firebase.analytics.FirebaseAnalytics.Param.ITEM_ID
import com.google.firebase.analytics.FirebaseAnalytics.Param.ITEM_NAME
import com.google.firebase.analytics.FirebaseAnalytics.getInstance
import com.supercilex.robotscouter.data.util.TeamHelper
import com.supercilex.robotscouter.ui.AuthHelper
import kotlin.properties.Delegates

private var analytics: FirebaseAnalytics by Delegates.notNull<FirebaseAnalytics>()

fun initAnalytics(context: Context) {
    analytics = getInstance(context)
}

fun logSelectTeamEvent(teamNumber: String) {
    val args = Bundle()
    args.putString(ITEM_ID, "select_team")
    args.putString(ITEM_NAME, teamNumber)
    args.putString(ITEM_CATEGORY, "team")
    analytics.logEvent(Event.VIEW_ITEM, args)
}

fun logEditTeamDetailsEvent(teamNumber: String) {
    val args = Bundle()
    args.putString(ITEM_ID, "edit_team_details")
    args.putString(ITEM_NAME, teamNumber)
    args.putString(ITEM_CATEGORY, "team")
    analytics.logEvent(Event.VIEW_ITEM, args)
}

fun logShareTeamEvent(teamNumber: String) {
    val args = Bundle()
    args.putString(ITEM_ID, "share_team")
    args.putString(ITEM_NAME, teamNumber)
    args.putString(ITEM_CATEGORY, "team")
    analytics.logEvent(Event.SHARE, args)
}

fun logExportTeamsEvent(teamHelpers: List<TeamHelper>) {
    val args = Bundle()
    args.putString(ITEM_ID, "export_teams")
    args.putString(ITEM_NAME, TeamHelper.getTeamNames(teamHelpers))
    args.putString(ITEM_CATEGORY, "teams")
    analytics.logEvent(Event.VIEW_ITEM, args)
}

fun logAddScoutEvent(teamNumber: String) {
    val args = Bundle()
    args.putString(ITEM_ID, "add_scout")
    args.putString(ITEM_NAME, teamNumber)
    args.putString(ITEM_CATEGORY, "scout")
    analytics.logEvent(Event.VIEW_ITEM, args)
}

fun logEditTemplateEvent(teamNumber: String) {
    val args = Bundle()
    args.putString(ITEM_ID, "edit_template")
    args.putString(ITEM_NAME, teamNumber)
    args.putString(ITEM_CATEGORY, "scout_template")
    analytics.logEvent(Event.VIEW_ITEM, args)
}

fun logLoginEvent() {
    analytics.logEvent(Event.LOGIN, Bundle())
    updateAnalyticsUserId()
}

fun updateAnalyticsUserId() {
    analytics.setUserId(AuthHelper.getUid())
}

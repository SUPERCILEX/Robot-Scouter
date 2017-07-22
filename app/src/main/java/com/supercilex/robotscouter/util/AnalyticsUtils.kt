package com.supercilex.robotscouter.util

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.FirebaseAnalytics.Event
import com.google.firebase.analytics.FirebaseAnalytics.Param.ITEM_CATEGORY
import com.google.firebase.analytics.FirebaseAnalytics.Param.ITEM_ID
import com.google.firebase.analytics.FirebaseAnalytics.Param.ITEM_NAME
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crash.FirebaseCrash
import com.supercilex.robotscouter.data.util.TeamHelper
import kotlin.properties.Delegates

private var analytics: FirebaseAnalytics by Delegates.notNull()

fun initAnalytics(context: Context) {
    analytics = FirebaseAnalytics.getInstance(context)
    FirebaseAuth.getInstance().addAuthStateListener {
        // Log uid to help debug db crashes
        FirebaseCrash.log("User id: ${getUid()}")
        analytics.setUserId(getUid())
    }
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
    args.putString(ITEM_CATEGORY, "teamsListener")
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

fun logLoginEvent() = analytics.logEvent(Event.LOGIN, Bundle())

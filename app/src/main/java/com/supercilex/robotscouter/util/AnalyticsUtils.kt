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
import com.supercilex.robotscouter.data.model.Team
import kotlin.properties.Delegates

private var analytics: FirebaseAnalytics by Delegates.notNull()

fun initAnalytics(context: Context) {
    analytics = FirebaseAnalytics.getInstance(context)
    FirebaseAuth.getInstance().addAuthStateListener {
        // Log uid to help debug db crashes
        FirebaseCrash.log("User id: $uid")
        analytics.setUserId(uid)
    }
}

fun logSelectTeamEvent(teamNumber: String) = analytics.logEvent(Event.VIEW_ITEM, Bundle().apply {
    putString(ITEM_ID, "select_team")
    putString(ITEM_NAME, teamNumber)
    putString(ITEM_CATEGORY, "team")
})

fun logEditTeamDetailsEvent(teamNumber: String) = analytics.logEvent(Event.VIEW_ITEM, Bundle().apply {
    putString(ITEM_ID, "edit_team_details")
    putString(ITEM_NAME, teamNumber)
    putString(ITEM_CATEGORY, "team")
})

fun logShareTeamEvent(teamNumber: String) = analytics.logEvent(Event.SHARE, Bundle().apply {
    putString(ITEM_ID, "share_team")
    putString(ITEM_NAME, teamNumber)
    putString(ITEM_CATEGORY, "team")
})

fun logExportTeamsEvent(teams: List<Team>) = analytics.logEvent(Event.VIEW_ITEM, Bundle().apply {
    putString(ITEM_ID, "export_teams")
    putString(ITEM_NAME, getTeamNames(teams))
    putString(ITEM_CATEGORY, "teams")
})

fun logAddScoutEvent(teamNumber: String) = analytics.logEvent(Event.VIEW_ITEM, Bundle().apply {
    putString(ITEM_ID, "add_scout")
    putString(ITEM_NAME, teamNumber)
    putString(ITEM_CATEGORY, "scout")
})

fun logEditTemplateEvent(teamNumber: String) = analytics.logEvent(Event.VIEW_ITEM, Bundle().apply {
    putString(ITEM_ID, "edit_template")
    putString(ITEM_NAME, teamNumber)
    putString(ITEM_CATEGORY, "scout_template")
})

fun logLoginEvent() = analytics.logEvent(Event.LOGIN, Bundle())

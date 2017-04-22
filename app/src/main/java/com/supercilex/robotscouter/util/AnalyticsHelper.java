package com.supercilex.robotscouter.util;

import android.content.Context;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.supercilex.robotscouter.data.util.TeamHelper;
import com.supercilex.robotscouter.ui.AuthHelper;

import java.util.List;

import static com.google.firebase.analytics.FirebaseAnalytics.Event;
import static com.google.firebase.analytics.FirebaseAnalytics.Param;

public class AnalyticsHelper {
    private static final AnalyticsHelper INSTANCE = new AnalyticsHelper();

    private FirebaseAnalytics mAnalytics;

    public static void init(Context context) {
        INSTANCE.mAnalytics = FirebaseAnalytics.getInstance(context);
    }

    public static void selectTeam(String teamNumber) {
        Bundle args = new Bundle();
        args.putString(Param.ITEM_ID, "select_team");
        args.putString(Param.ITEM_NAME, teamNumber);
        args.putString(Param.ITEM_CATEGORY, "team");
        INSTANCE.mAnalytics.logEvent(Event.VIEW_ITEM, args);
    }

    public static void editTeamDetails(String teamNumber) {
        Bundle args = new Bundle();
        args.putString(Param.ITEM_ID, "edit_team_details");
        args.putString(Param.ITEM_NAME, teamNumber);
        args.putString(Param.ITEM_CATEGORY, "team");
        INSTANCE.mAnalytics.logEvent(Event.VIEW_ITEM, args);
    }

    public static void shareTeam(String teamNumber) {
        Bundle args = new Bundle();
        args.putString(Param.ITEM_ID, "share_team");
        args.putString(Param.ITEM_NAME, teamNumber);
        args.putString(Param.ITEM_CATEGORY, "team");
        INSTANCE.mAnalytics.logEvent(Event.SHARE, args);
    }

    public static void exportTeams(List<TeamHelper> teamHelpers) {
        Bundle args = new Bundle();
        args.putString(Param.ITEM_ID, "export_teams");
        args.putString(Param.ITEM_NAME, TeamHelper.getTeamNames(teamHelpers));
        args.putString(Param.ITEM_CATEGORY, "teams");
        INSTANCE.mAnalytics.logEvent(Event.VIEW_ITEM, args);
    }

    public static void addScout(String teamNumber) {
        Bundle args = new Bundle();
        args.putString(Param.ITEM_ID, "add_scout");
        args.putString(Param.ITEM_NAME, teamNumber);
        args.putString(Param.ITEM_CATEGORY, "scout");
        INSTANCE.mAnalytics.logEvent(Event.VIEW_ITEM, args);
    }

    public static void editTemplate(String teamNumber) {
        Bundle args = new Bundle();
        args.putString(Param.ITEM_ID, "edit_template");
        args.putString(Param.ITEM_NAME, teamNumber);
        args.putString(Param.ITEM_CATEGORY, "scout_template");
        INSTANCE.mAnalytics.logEvent(Event.VIEW_ITEM, args);
    }

    public static void login() {
        INSTANCE.mAnalytics.logEvent(Event.LOGIN, new Bundle());
        updateUserId();
    }

    public static void updateUserId() {
        String uid = AuthHelper.getUid();
        INSTANCE.mAnalytics.setUserId(uid);
        INSTANCE.mAnalytics.setUserProperty("user_id", uid);
    }
}

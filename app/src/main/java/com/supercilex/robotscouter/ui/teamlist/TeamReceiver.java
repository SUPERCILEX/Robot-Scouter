package com.supercilex.robotscouter.ui.teamlist;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.appinvite.AppInvite;
import com.google.android.gms.appinvite.AppInviteInvitationResult;
import com.google.android.gms.appinvite.AppInviteReferral;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.supercilex.robotscouter.data.model.Scout;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.ui.scout.ScoutActivity;

import java.util.List;

public class TeamReceiver implements ResultCallback<AppInviteInvitationResult> {
    public static final String TEAM_QUERY_KEY = "team";
    public static final String SCOUT_QUERY_KEY = "scout";
    private static final String UTM_SOURCE = "utm_source";
    private static final String UTM_SOURCE_VALUE = "robotscouter";

    public static final String APP_LINK_BASE =
            "https://supercilex.github.io/?" +
                    TeamReceiver.UTM_SOURCE + "=" + TeamReceiver.UTM_SOURCE_VALUE +
                    "&" + TeamReceiver.TEAM_QUERY_KEY + "=";

    private FragmentActivity mActivity;

    private TeamReceiver(FragmentActivity activity) {
        mActivity = activity;

        // Check for deep links
        AppInvite.AppInviteApi
                .getInvitation(new GoogleApiClient.Builder(mActivity)
                                       .enableAutoManage(mActivity, null)
                                       .addApi(AppInvite.API)
                                       .build(),
                               null,
                               false)
                .setResultCallback(this);
    }

    public static TeamReceiver init(FragmentActivity activity) {
        return new TeamReceiver(activity);
    }

    @Override
    public void onResult(@NonNull AppInviteInvitationResult result) {
        Uri deepLink = mActivity.getIntent().getData();
        // Consume intent
        mActivity.setIntent(new Intent());
        if (deepLink == null
                || !deepLink.getQueryParameter(UTM_SOURCE).equals(UTM_SOURCE_VALUE)
                || deepLink.getQueryParameter(TEAM_QUERY_KEY) == null) {
            return; // Nothing to see here
        }

        // Received invite from Firebase dynamic links
        if (result.getStatus().isSuccess()) {
            Team team = getTeam(Uri.parse(AppInviteReferral.getDeepLink(result.getInvitationIntent())));

            Team.getIndicesRef().addListenerForSingleValueEvent(
                    new CheckTeamExistsListener(team.getKey(), team.getNumber()));

            List<String> scouts = deepLink.getQueryParameters(SCOUT_QUERY_KEY);
            for (String scoutKey : scouts) {
                Scout.getIndicesRef()
                        .child(scoutKey)
                        .setValue(team.getNumberAsLong());
            }
        } else { // Received normal intent
            ScoutActivity.start(mActivity, getTeam(deepLink), false);
        }
    }

    private Team getTeam(Uri deepLink) {
        // Format: -key:2521
        String[] team = deepLink.getQueryParameter(TEAM_QUERY_KEY).split(":");
        String teamKey = team[0];
        String teamNumber = team[1];

        return new Team.Builder(teamNumber).setKey(teamKey).build();
    }

    private class CheckTeamExistsListener implements ValueEventListener {
        private String mTeamKey;
        private String mTeamNumber;

        public CheckTeamExistsListener(String teamKey, String teamNumber) {
            mTeamKey = teamKey;
            mTeamNumber = teamNumber;
        }

        @Override
        public void onDataChange(DataSnapshot snapshot) {
            for (DataSnapshot teamKey : snapshot.getChildren()) {
                if (teamKey.getValue().equals(mTeamNumber)) {
                    launchTeam();
                    return;
                }
            }

            Long number = Long.valueOf(mTeamNumber);
            Team.getIndicesRef().child(mTeamKey).setValue(number, number);
            launchTeam();
        }

        @Override
        public void onCancelled(DatabaseError error) {
            FirebaseCrash.report(error.toException());
        }

        private void launchTeam() {
            ScoutActivity.start(mActivity,
                                new Team.Builder(mTeamNumber).setKey(mTeamKey).build(),
                                false);
        }
    }
}

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
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.ui.scout.ScoutActivity;

public final class TeamReceiver implements ResultCallback<AppInviteInvitationResult> {
    private static final String TEAM_QUERY_KEY = "team";
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
        if (deepLink == null
                || !deepLink.getQueryParameter(UTM_SOURCE).equals(UTM_SOURCE_VALUE)
                || deepLink.getQueryParameter(TEAM_QUERY_KEY) == null) {
            return; // Nothing to see here
        }

        // Consume intent
        mActivity.setIntent(new Intent());

        // Received invite from Firebase dynamic links
        if (result.getStatus().isSuccess()) {
            Team team = getTeam(Uri.parse(AppInviteReferral.getDeepLink(result.getInvitationIntent())));
            Long number = team.getNumberAsLong();
            Team.getIndicesRef().child(team.getKey()).setValue(number, number);
            launchTeam(team);
        } else { // Received normal intent
            launchTeam(getTeam(deepLink));
        }
    }

    private Team getTeam(Uri deepLink) {
        // Format: -key:2521
        String[] team = deepLink.getQueryParameter(TEAM_QUERY_KEY).split(":");
        String teamKey = team[0];
        String teamNumber = team[1];

        return new Team.Builder(teamNumber).setKey(teamKey).build();
    }

    private void launchTeam(Team team) {
        ScoutActivity.start(mActivity, team, false);
    }
}

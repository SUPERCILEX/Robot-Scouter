package com.supercilex.robotscouter.ui.teamlist;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;

import com.firebase.ui.auth.util.GoogleApiHelper;
import com.google.android.gms.appinvite.AppInvite;
import com.google.android.gms.appinvite.AppInviteInvitationResult;
import com.google.android.gms.appinvite.AppInviteReferral;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.data.util.TeamHelper;
import com.supercilex.robotscouter.ui.scout.ScoutActivity;
import com.supercilex.robotscouter.util.Constants;

import java.util.ArrayList;
import java.util.List;

public class TeamReceiver implements ResultCallback<AppInviteInvitationResult> {
    public static final String TEAM_QUERY_KEY = "team";
    private static final String UTM_SOURCE = "utm_source";
    private static final String UTM_SOURCE_VALUE = "robotscouter";
    public static final String APP_LINK_BASE = "https://supercilex.github.io/?" + TeamReceiver.UTM_SOURCE + "=" + TeamReceiver.UTM_SOURCE_VALUE;

    private FragmentActivity mActivity;

    protected TeamReceiver(FragmentActivity activity) {
        mActivity = activity;

        // Check for deep links
        AppInvite.AppInviteApi
                .getInvitation(new GoogleApiClient.Builder(mActivity)
                                       .enableAutoManage(mActivity,
                                                         GoogleApiHelper.getSafeAutoManageId(),
                                                         null /* listener */)
                                       .addApi(AppInvite.API)
                                       .build(),
                               mActivity,
                               false)
                .setResultCallback(this);
    }

    public static TeamReceiver init(FragmentActivity activity) {
        return new TeamReceiver(activity);
    }

    @Override
    public void onResult(@NonNull AppInviteInvitationResult result) {
        if (result.getStatus().isSuccess()) { // Received invite from Firebase dynamic links
            List<Team> teams = getTeam(Uri.parse(AppInviteReferral.getDeepLink(result.getInvitationIntent())));
            for (Team team : teams) {
                long number = team.getNumberAsLong();
                TeamHelper.getIndicesRef().child(team.getKey()).setValue(number, number);
            }
            if (teams.size() == Constants.SINGLE_ITEM) {
                launchTeam(teams.get(0));
            } else {
                Snackbar.make(mActivity.findViewById(R.id.root),
                              R.string.teams_imported,
                              Snackbar.LENGTH_LONG)
                        .show();
            }
        } else { // Received normal intent
            Uri deepLink = mActivity.getIntent().getData();
            if (deepLink == null
                    || !deepLink.getQueryParameter(UTM_SOURCE).equals(UTM_SOURCE_VALUE)
                    || deepLink.getQueryParameter(TEAM_QUERY_KEY) == null) {
                return; // Nothing to see here
            }

            launchTeam(getTeam(deepLink).get(0));
        }
    }

    private List<Team> getTeam(Uri deepLink) {
        List<Team> teams = new ArrayList<>();
        for (String teamPair : deepLink.getQueryParameters(TEAM_QUERY_KEY)) {
            // Format: -key:2521
            String[] teamPairSplit = teamPair.split(":");
            String teamKey = teamPairSplit[0];
            String teamNumber = teamPairSplit[1];

            teams.add(new Team.Builder(teamNumber).setKey(teamKey).build()); // NOPMD
        }
        return teams;
    }

    private void launchTeam(Team team) {
        ScoutActivity.start(mActivity, team.getHelper(), false);

        // Consume intent
        mActivity.setIntent(new Intent());
    }
}

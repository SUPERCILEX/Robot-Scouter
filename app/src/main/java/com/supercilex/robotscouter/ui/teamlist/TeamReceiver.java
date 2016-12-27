package com.supercilex.robotscouter.ui.teamlist;

import android.content.Intent;
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

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeamReceiver implements ResultCallback<AppInviteInvitationResult> {
    public static final String UTM_SOURCE = "utm_source";
    public static final String UTM_SOURCE_VALUE = "robotscouter";
    public static final String TEAM_KEY = "team";
    public static final String SCOUT_KEY = "scout";

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
        if (result.getStatus().isSuccess()) {
            String deepLink = AppInviteReferral.getDeepLink(result.getInvitationIntent());
            Map<String, List<String>> queryParams = splitQuery(deepLink);

            if (queryParams.get(UTM_SOURCE).get(0).equals(UTM_SOURCE_VALUE)) {
                // Consume intent
                mActivity.setIntent(new Intent());

                // Format: -key:2521
                String[] team = queryParams.get(TEAM_KEY).get(0).split(":");
                String teamKey = team[0];
                String teamNumber = team[1];
                Team.getIndicesRef().addListenerForSingleValueEvent(
                        new CheckTeamExistsListener(teamKey, teamNumber));

                List<String> scouts = queryParams.get(SCOUT_KEY);
                for (String scoutKey : scouts) {
                    Scout.getIndicesRef()
                            .child(scoutKey)
                            .setValue(Long.valueOf(teamNumber));
                }
            }
        }
    }

    private Map<String, List<String>> splitQuery(String url) {
        Map<String, List<String>> queryPairs = new HashMap<>();
        String[] pairs = new String[0];
        try {
            pairs = new URL(url).getQuery().split("&");
        } catch (MalformedURLException e) {
            FirebaseCrash.report(e);
        }

        try {
            for (String pair : pairs) {
                int index = pair.indexOf('=');
                String key = index > 0
                        ? URLDecoder.decode(pair.substring(0, index), "UTF-8") : pair;

                if (!queryPairs.containsKey(key)) {
                    queryPairs.put(key, new ArrayList<String>()); // NOPMD
                }

                String value = index > 0 && pair.length() > index + 1
                        ? URLDecoder.decode(pair.substring(index + 1), "UTF-8") : "";
                queryPairs.get(key).add(value);
            }
        } catch (UnsupportedEncodingException e) {
            FirebaseCrash.report(e);
        }

        return queryPairs;
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

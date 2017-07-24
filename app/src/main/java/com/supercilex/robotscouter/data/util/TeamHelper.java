package com.supercilex.robotscouter.data.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.firebase.appindexing.Action;
import com.google.firebase.appindexing.FirebaseAppIndex;
import com.google.firebase.appindexing.FirebaseUserActions;
import com.google.firebase.appindexing.Indexable;
import com.google.firebase.appindexing.builders.DigitalDocumentBuilder;
import com.google.firebase.appindexing.builders.Indexables;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.util.Constants;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.supercilex.robotscouter.data.client.DownloadTeamDataJobKt.startDownloadTeamDataJob;
import static com.supercilex.robotscouter.data.util.ScoutUtilsKt.deleteAllScouts;
import static com.supercilex.robotscouter.util.AuthUtilsKt.getUid;
import static com.supercilex.robotscouter.util.ConstantsKt.FIREBASE_TEMPLATE_KEY;
import static com.supercilex.robotscouter.util.ConstantsKt.FIREBASE_TIMESTAMP;
import static com.supercilex.robotscouter.util.ConstantsKt.KEY_QUERY;
import static com.supercilex.robotscouter.util.ConstantsKt.SINGLE_ITEM;
import static com.supercilex.robotscouter.util.ConstantsKt.TEAMS_LINK_BASE;
import static com.supercilex.robotscouter.util.ConstantsKt.TWO_ITEMS;
import static com.supercilex.robotscouter.util.ConstantsKt.getFIREBASE_TEAMS;
import static com.supercilex.robotscouter.util.ConstantsKt.getFIREBASE_TEAM_INDICES;
import static com.supercilex.robotscouter.util.CustomTabsUtilsKt.launchUrl;
import static com.supercilex.robotscouter.util.RemoteConfigUtilsKt.fetchAndActivate;

public class TeamHelper implements Parcelable, Comparable<TeamHelper> {
    public static final Parcelable.Creator<TeamHelper> CREATOR = new Parcelable.Creator<TeamHelper>() {
        @Override
        public TeamHelper createFromParcel(Parcel source) {
            return new TeamHelper(source.readParcelable(Team.class.getClassLoader()));
        }

        @Override
        public TeamHelper[] newArray(int size) {
            return new TeamHelper[size];
        }
    };

    private static final String TEAM_HELPER_KEY = "com.supercilex.robotscouter.data.util.TeamHelper";
    private static final String TEAM_HELPERS_KEY = "com.supercilex.robotscouter.data.util.TeamHelpers";
    private static final String FRESHNESS_DAYS = "team_freshness";

    private final Team mTeam;

    public TeamHelper(@NonNull Team team) {
        mTeam = team;
    }

    public static DatabaseReference getIndicesRef() {
        return getFIREBASE_TEAM_INDICES().child(getUid());
    }

    public static TeamHelper parse(Intent intent) {
        return intent.getParcelableExtra(TEAM_HELPER_KEY);
    }

    public static TeamHelper parse(Bundle args) {
        return args.getParcelable(TEAM_HELPER_KEY);
    }

    public static List<TeamHelper> parseList(Intent intent) {
        return intent.getParcelableArrayListExtra(TEAM_HELPERS_KEY);
    }

    public static List<TeamHelper> parseList(Bundle args) {
        return args.getParcelableArrayList(TEAM_HELPERS_KEY);
    }

    public static Intent toIntent(List<TeamHelper> teamHelpers) {
        return new Intent().putExtra(TEAM_HELPERS_KEY, new ArrayList<>(teamHelpers));
    }

    public static Bundle toBundle(List<TeamHelper> teamHelpers) {
        Bundle args = new Bundle();
        args.putParcelableArrayList(TEAM_HELPERS_KEY, new ArrayList<>(teamHelpers));
        return args;
    }

    public static String getTeamNames(List<TeamHelper> teamHelpers) {
        List<TeamHelper> sortedTeamHelpers = new ArrayList<>(teamHelpers);
        Collections.sort(sortedTeamHelpers);

        String teamName;

        if (sortedTeamHelpers.size() == SINGLE_ITEM) {
            teamName = sortedTeamHelpers.get(0).toString();
        } else if (sortedTeamHelpers.size() == TWO_ITEMS) {
            teamName = sortedTeamHelpers.get(0) + " and " + sortedTeamHelpers.get(1);
        } else {
            boolean teamsMaxedOut = sortedTeamHelpers.size() > 10;
            int size = teamsMaxedOut ? 10 : sortedTeamHelpers.size();

            StringBuilder names = new StringBuilder(4 * size);
            for (int i = 0; i < size; i++) {
                names.append(sortedTeamHelpers.get(i).getTeam().getNumber());
                if (i < size - 1) names.append(", ");
                if (i == size - 2 && !teamsMaxedOut) names.append("and ");
            }

            if (teamsMaxedOut) names.append(" and more");

            teamName = names.toString();
        }

        return teamName;
    }

    public Bundle toBundle() {
        Bundle args = new Bundle();
        args.putParcelable(TEAM_HELPER_KEY, this);
        return args;
    }

    public DatabaseReference getRef() {
        return getFIREBASE_TEAMS().child(mTeam.getKey());
    }

    public Team getTeam() {
        return mTeam;
    }

    public void addTeam() {
        DatabaseReference index = getIndicesRef().push();
        mTeam.setKey(index.getKey());
        Long number = mTeam.getNumberAsLong();
        index.setValue(number, number);

        if (!Constants.sFirebaseScoutTemplates.isEmpty()) {
            mTeam.setTemplateKey(Constants.sFirebaseScoutTemplates.get(0).getKey());
        }
        forceUpdateTeam();
        forceRefresh();

        FirebaseUserActions.getInstance()
                .end(new Action.Builder(Action.Builder.ADD_ACTION)
                             .setObject(toString(), getDeepLink())
                             .build());
    }

    public void updateTeam(Team newTeam) {
        checkForMatchingTeamDetails(newTeam);
        if (mTeam.equals(newTeam)) {
            getRef().child(FIREBASE_TIMESTAMP).setValue(mTeam.getCurrentTimestamp());
            return;
        }

        if (mTeam.getHasCustomName() == null) mTeam.setName(newTeam.getName());
        if (mTeam.getHasCustomMedia() == null) {
            mTeam.setMedia(newTeam.getMedia());
            mTeam.setMediaYear(newTeam.getMediaYear());
        }
        if (mTeam.getHasCustomWebsite() == null) mTeam.setWebsite(newTeam.getWebsite());
        forceUpdateTeam();
    }

    private void checkForMatchingTeamDetails(Team newTeam) {
        if (TextUtils.equals(mTeam.getName(), newTeam.getName())) {
            mTeam.setHasCustomName(false);
        }
        if (TextUtils.equals(mTeam.getMedia(), newTeam.getMedia())) {
            mTeam.setHasCustomMedia(false);
        }
        if (TextUtils.equals(mTeam.getWebsite(), newTeam.getWebsite())) {
            mTeam.setHasCustomWebsite(false);
        }
    }

    public void updateMedia(Team newTeam) {
        mTeam.setMedia(newTeam.getMedia());
        mTeam.setShouldUploadMediaToTba(false);
        forceUpdateTeam();
    }

    public void forceUpdateTeam() {
        getRef().setValue(mTeam);
        FirebaseAppIndex.getInstance().update(getIndexable());
    }

    public void forceRefresh() {
        getRef().child(FIREBASE_TIMESTAMP).removeValue();
    }

    public void copyMediaInfo(TeamHelper newHelper) {
        Team currentTeam = getTeam();
        Team newTeam = newHelper.getTeam();

        currentTeam.setMedia(newTeam.getMedia());
        currentTeam.setShouldUploadMediaToTba(newTeam.getShouldUploadMediaToTba() != null);
        currentTeam.setMediaYear(Calendar.getInstance().get(Calendar.YEAR));
    }

    public void updateTemplateKey(String key) {
        mTeam.setTemplateKey(key);
        getRef().child(FIREBASE_TEMPLATE_KEY).setValue(mTeam.getTemplateKey());
    }

    public void deleteTeam() {
        deleteAllScouts(mTeam.getKey()).addOnSuccessListener(aVoid -> {
            getRef().removeValue();
            getIndicesRef().child(mTeam.getKey()).removeValue();
            FirebaseAppIndex.getInstance().remove(getDeepLink());
        });
    }

    public void fetchLatestData(Context context) {
        Context appContext = context.getApplicationContext();
        fetchAndActivate().addOnSuccessListener(nothing -> {
            long differenceDays =
                    TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - mTeam.getTimestamp());
            double freshness = FirebaseRemoteConfig.getInstance().getDouble(FRESHNESS_DAYS);

            if (differenceDays >= freshness) {
                startDownloadTeamDataJob(appContext, this);
            }
        });
    }

    public boolean isOutdatedMedia() {
        return mTeam.getMediaYear() < Calendar.getInstance().get(Calendar.YEAR)
                || TextUtils.isEmpty(mTeam.getMedia());
    }

    public void visitTbaWebsite(Context context) {
        Uri tbaUrl = Uri.parse("https://www.thebluealliance.com/team/" + mTeam.getNumber());
        launchUrl(context, tbaUrl);
    }

    public void visitTeamWebsite(Context context) {
        launchUrl(context, Uri.parse(mTeam.getWebsite()));
    }

    public Indexable getIndexable() {
        DigitalDocumentBuilder builder = Indexables.digitalDocumentBuilder()
                .setUrl(getDeepLink())
                .setName(toString())
                .setMetadata(new Indexable.Metadata.Builder().setWorksOffline(true));
        if (mTeam.getMedia() != null) builder.setImage(mTeam.getMedia());
        return builder.build();
    }

    private String getDeepLink() {
        return TEAMS_LINK_BASE + "?" + getLinkKeyNumberPair();
    }

    public String getLinkKeyNumberPair() {
        return "&" + KEY_QUERY + "=" + mTeam.getKey() + ":" + mTeam.getNumber();
    }

    public Action getViewAction() {
        return new Action.Builder(Action.Builder.VIEW_ACTION)
                .setObject(toString(), getDeepLink())
                .build();
    }

    @Override
    public int compareTo(TeamHelper o) {
        return mTeam.compareTo(o.getTeam());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TeamHelper helper = (TeamHelper) o;

        return mTeam.equals(helper.mTeam);
    }

    @Override
    public int hashCode() {
        return mTeam.hashCode();
    }

    @Override
    public String toString() {
        return mTeam.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mTeam, flags);
    }
}

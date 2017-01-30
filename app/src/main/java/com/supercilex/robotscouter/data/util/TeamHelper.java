package com.supercilex.robotscouter.data.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.appindexing.Action;
import com.google.firebase.appindexing.FirebaseAppIndex;
import com.google.firebase.appindexing.FirebaseUserActions;
import com.google.firebase.appindexing.Indexable;
import com.google.firebase.appindexing.builders.DigitalDocumentBuilder;
import com.google.firebase.appindexing.builders.Indexables;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.supercilex.robotscouter.data.client.DownloadTeamDataJob;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.ui.AuthHelper;
import com.supercilex.robotscouter.ui.teamlist.TeamReceiver;
import com.supercilex.robotscouter.util.Constants;
import com.supercilex.robotscouter.util.CustomTabsHelper;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class TeamHelper implements Parcelable, Comparable<TeamHelper> {
    public static final Parcelable.Creator<TeamHelper> CREATOR = new Parcelable.Creator<TeamHelper>() {
        @Override
        public TeamHelper createFromParcel(Parcel source) {
            return new TeamHelper((Team) source.readParcelable(Team.class.getClassLoader()));
        }

        @Override
        public TeamHelper[] newArray(int size) {
            return new TeamHelper[size];
        }
    };

    private static final String INTENT_TEAM = "com.supercilex.robotscouter.Team";
    private static final String SCOUT_TEMPLATE = "com.supercilex.robotscouter.scout_template";
    private static final int FRESHNESS_DAYS = 4;

    private final Team mTeam;

    public TeamHelper(Team team) {
        mTeam = team;
    }

    public static DatabaseReference getIndicesRef() {
        return Constants.FIREBASE_TEAM_INDICES.child(AuthHelper.getUid());
    }

    public static TeamHelper get(Intent intent) {
        return (TeamHelper) intent.getParcelableExtra(INTENT_TEAM);
    }


    public static TeamHelper get(Bundle arguments) {
        return (TeamHelper) arguments.getParcelable(INTENT_TEAM);
    }

    public Intent getIntent() {
        return new Intent().putExtra(INTENT_TEAM, this);
    }


    public Bundle getBundle() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(INTENT_TEAM, this);
        return bundle;
    }


    public DatabaseReference getRef() {
        return Constants.FIREBASE_TEAMS.child(mTeam.getKey());
    }

    public Team getTeam() {
        return mTeam;
    }

    public String getFormattedName() {
        return TextUtils.isEmpty(mTeam.getName())
                ? mTeam.getNumber() : mTeam.getNumber() + " - " + mTeam.getName();
    }

    public void addTeam(Context context) {
        DatabaseReference index = getIndicesRef().push();
        mTeam.setKey(index.getKey());
        Long number = mTeam.getNumberAsLong();
        index.setValue(number, number);
        mTeam.setTemplateKey(context.getSharedPreferences(SCOUT_TEMPLATE, Context.MODE_PRIVATE)
                                     .getString(SCOUT_TEMPLATE, null));
        forceUpdateTeam();
        FirebaseUserActions.getInstance()
                .end(new Action.Builder(Action.Builder.ADD_ACTION)
                             .setObject(getFormattedName(), getDeepLink())
                             .build());
    }

    public void updateTeam(Team newTeam) {
        if (mTeam.equals(newTeam)) {
            getRef().child(Constants.FIREBASE_TIMESTAMP).setValue(mTeam.getServerTimestamp());
            return;
        }

        checkForMatchingTeamDetails(newTeam);
        if (mTeam.getHasCustomName() == null) mTeam.setName(newTeam.getName());
        if (mTeam.getHasCustomMedia() == null) mTeam.setMedia(newTeam.getMedia());
        if (mTeam.getHasCustomWebsite() == null) mTeam.setWebsite(newTeam.getWebsite());
        forceUpdateTeam();
    }

    public void forceUpdateTeam() {
        getRef().setValue(mTeam);
        FirebaseAppIndex.getInstance().update(getIndexable());
    }

    private void checkForMatchingTeamDetails(Team newTeam) {
        if (mTeam.getHasCustomName() != null && mTeam.getName().equals(newTeam.getName())) {
            mTeam.setHasCustomName(false);
        }
        if (mTeam.getHasCustomMedia() != null && mTeam.getMedia().equals(newTeam.getMedia())) {
            mTeam.setHasCustomMedia(false);
        }
        if (mTeam.getHasCustomWebsite() != null
                && mTeam.getWebsite().equals(newTeam.getWebsite())) {
            mTeam.setHasCustomWebsite(false);
        }
    }

    public void updateTemplateKey(final String key, Context context) {
        mTeam.setTemplateKey(key);
        getRef().child(Constants.FIREBASE_TEMPLATE_KEY).setValue(mTeam.getTemplateKey());
        context.getSharedPreferences(SCOUT_TEMPLATE, Context.MODE_PRIVATE)
                .edit()
                .putString(SCOUT_TEMPLATE, key)
                .apply();
        TeamIndices.getAll().addOnSuccessListener(new OnSuccessListener<List<DataSnapshot>>() {
            private final ValueEventListener mTeamTemplateUpdater = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    DataSnapshot templateSnapshot = snapshot.child(Constants.FIREBASE_TEMPLATE_KEY);
                    if (templateSnapshot.getValue() == null) {
                        templateSnapshot.getRef().setValue(key);
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    FirebaseCrash.report(error.toException());
                }
            };

            @Override
            public void onSuccess(List<DataSnapshot> snapshots) {
                for (DataSnapshot snapshot : snapshots) {
                    Constants.FIREBASE_TEAMS
                            .child(snapshot.getKey())
                            .addListenerForSingleValueEvent(mTeamTemplateUpdater);
                }
            }
        });
    }

    public void deleteTeam(Context context) {
        final Context appContext = context.getApplicationContext();
        ScoutUtils.deleteAll(mTeam.getKey()).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                getRef().removeValue();
                getIndicesRef().child(mTeam.getKey()).removeValue();
                if (mTeam.getTemplateKey() != null) {
                    TeamIndices.getAll()
                            .addOnSuccessListener(new OnSuccessListener<List<DataSnapshot>>() {
                                @Override
                                public void onSuccess(List<DataSnapshot> snapshots) {
                                    if (snapshots.isEmpty()) {
                                        Constants.FIREBASE_SCOUT_TEMPLATES
                                                .child(mTeam.getTemplateKey())
                                                .removeValue();
                                        appContext.getSharedPreferences(SCOUT_TEMPLATE,
                                                                        Context.MODE_PRIVATE)
                                                .edit()
                                                .clear()
                                                .apply();
                                    }
                                }
                            });
                }
                FirebaseAppIndex.getInstance().remove(getDeepLink());
            }
        });
    }

    public void fetchLatestData(Context context) {
        long differenceDays = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - mTeam.getTimestamp());
        if (differenceDays >= FRESHNESS_DAYS) DownloadTeamDataJob.start(context, this);
    }

    public void visitTbaWebsite(Context context) {
        Uri tbaUrl = Uri.parse("https://www.thebluealliance.com/team/" + mTeam.getNumber());
        CustomTabsHelper.launchUrl(context, tbaUrl);
    }

    public void visitTeamWebsite(Context context) {
        CustomTabsHelper.launchUrl(context, Uri.parse(mTeam.getWebsite()));
    }

    public Indexable getIndexable() {
        DigitalDocumentBuilder builder = Indexables.digitalDocumentBuilder()
                .setUrl(getDeepLink())
                .setName(getFormattedName())
                .setMetadata(new Indexable.Metadata.Builder().setWorksOffline(true));
        if (mTeam.getMedia() != null) builder.setImage(mTeam.getMedia());
        return builder.build();
    }

    private String getDeepLink() {
        return TeamReceiver.APP_LINK_BASE + getLinkKeyNumberPair();
    }

    public String getLinkKeyNumberPair() {
        return "&" + TeamReceiver.TEAM_QUERY_KEY + "=" + mTeam.getKey() + ":" + mTeam.getNumber();
    }

    public Action getViewAction() {
        return new Action.Builder(Action.Builder.VIEW_ACTION)
                .setObject(getFormattedName(), getDeepLink())
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
        return "TeamHelper{" +
                "mTeam=" + mTeam +
                '}';
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

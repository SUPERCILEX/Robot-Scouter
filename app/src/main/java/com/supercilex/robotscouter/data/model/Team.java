package com.supercilex.robotscouter.data.model; // NOPMD

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.text.TextUtils;

import com.firebase.ui.auth.util.Preconditions;
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
import com.google.firebase.database.Exclude;
import com.google.firebase.database.PropertyName;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.supercilex.robotscouter.data.TeamIndices;
import com.supercilex.robotscouter.data.client.DownloadTeamDataJob;
import com.supercilex.robotscouter.data.client.DownloadTeamDataJob21;
import com.supercilex.robotscouter.ui.AuthHelper;
import com.supercilex.robotscouter.ui.teamlist.TeamReceiver;
import com.supercilex.robotscouter.util.BaseHelper;
import com.supercilex.robotscouter.util.Constants;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class Team implements Parcelable, Comparable<Team> {
    @Exclude
    public static final Creator<Team> CREATOR = new Creator<Team>() {
        @Override
        public Team createFromParcel(Parcel source) {
            return new Team(source.readString(),
                            source.readString(),
                            source.readString(),
                            source.readString(),
                            source.readString(),
                            source.readString(),
                            getBooleanForInt(source.readInt()),
                            getBooleanForInt(source.readInt()),
                            getBooleanForInt(source.readInt()),
                            source.readLong());
        }

        @Override
        public Team[] newArray(int size) {
            return new Team[size];
        }

        private boolean getBooleanForInt(int value) {
            return value == 1;
        }
    };

    @Exclude private static final String INTENT_TEAM = "com.supercilex.robotscouter.Team";

    @Exclude private static final String FIREBASE_TIMESTAMP = "timestamp";
    @Exclude private static final String FIREBASE_TEMPLATE_KEY = "templateKey";
    @Exclude private static final String SCOUT_TEMPLATE = "com.supercilex.robotscouter.scout_template";

    @Exclude private static final int FRESHNESS_DAYS = 4;

    @Exclude private String mNumber;
    @Exclude private String mKey;
    @Exclude private String mTemplateKey;
    @Exclude private String mName;
    @Exclude private String mMedia;
    @Exclude private String mWebsite;
    @Exclude private boolean mHasCustomName;
    @Exclude private boolean mHasCustomMedia;
    @Exclude private boolean mHasCustomWebsite;
    @Exclude private long mTimestamp;

    @RestrictTo(RestrictTo.Scope.TESTS)
    public Team() {
        // Needed for Firebase
    }

    private Team(String number,
                 String key,
                 String templateKey,
                 String name,
                 String media,
                 String website,
                 boolean hasCustomName,
                 boolean hasCustomMedia,
                 boolean hasCustomWebsite,
                 long timestamp) {
        mNumber = number;
        mKey = key;
        mTemplateKey = templateKey;
        mName = name;
        mMedia = media;
        mWebsite = website;
        mHasCustomName = hasCustomName;
        mHasCustomMedia = hasCustomMedia;
        mHasCustomWebsite = hasCustomWebsite;
        mTimestamp = timestamp;
    }

    @Exclude
    public static DatabaseReference getIndicesRef() {
        return Constants.FIREBASE_TEAM_INDICES.child(AuthHelper.getUid());
    }

    @Exclude
    public static Team getTeam(Intent intent) {
        return (Team) Preconditions.checkNotNull(intent.getParcelableExtra(INTENT_TEAM),
                                                 "Team cannot be null");
    }

    @Exclude
    public static Team getTeam(Bundle arguments) {
        return (Team) Preconditions.checkNotNull(arguments.getParcelable(INTENT_TEAM),
                                                 "Team cannot be null");
    }

    @Exclude
    public Intent getIntent() {
        return new Intent().putExtra(INTENT_TEAM, this);
    }

    @Exclude
    public Bundle getBundle() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(INTENT_TEAM, this);
        return bundle;
    }

    @Exclude
    public DatabaseReference getRef() {
        return Constants.FIREBASE_TEAMS.child(mKey);
    }

    @Keep
    @NonNull
    public String getNumber() {
        return mNumber;
    }

    @Keep
    @RestrictTo(RestrictTo.Scope.TESTS)
    public void setNumber(String number) {
        mNumber = number;
    }

    @Exclude
    public long getNumberAsLong() {
        return Long.parseLong(mNumber);
    }

    @Exclude
    public String getKey() {
        return mKey;
    }

    @Exclude
    public void setKey(String key) {
        mKey = key;
    }

    @Keep
    public String getTemplateKey() {
        return mTemplateKey;
    }

    @Keep
    @RestrictTo(RestrictTo.Scope.TESTS)
    public void setTemplateKey(String templateKey) {
        mTemplateKey = templateKey;
    }

    @Keep
    public String getName() {
        return mName;
    }

    @Keep
    public void setName(String name) {
        mName = name;
    }

    @Exclude
    @NonNull
    public String getFormattedName() {
        return TextUtils.isEmpty(getName()) ? getNumber() : getNumber() + " - " + getName();
    }

    @Keep
    public String getMedia() {
        return mMedia;
    }

    @Keep
    public void setMedia(String media) {
        mMedia = media;
    }


    @Keep
    public String getWebsite() {
        return mWebsite;
    }

    @Keep
    public void setWebsite(String website) {
        mWebsite = website;
    }

    // The following methods return Boolean to prevent Firebase from adding useless false values:

    @Keep
    @Nullable
    public Boolean getHasCustomName() {
        if (mHasCustomName) {
            return true;
        } else {
            return null;
        }
    }

    @Keep
    public void setHasCustomName() {
        mHasCustomName = true;
    }

    @Keep
    @Nullable
    public Boolean getHasCustomMedia() {
        if (mHasCustomMedia) {
            return true;
        } else {
            return null;
        }
    }

    @Keep
    public void setHasCustomMedia() {
        mHasCustomMedia = true;
    }

    @Keep
    @Nullable
    public Boolean getHasCustomWebsite() {
        if (mHasCustomWebsite) {
            return true;
        } else {
            return null;
        }
    }

    @Keep
    public void setHasCustomWebsite() {
        mHasCustomWebsite = true;
    }

    @Keep
    @PropertyName(FIREBASE_TIMESTAMP)
    public Object getServerTimestamp() {
        return ServerValue.TIMESTAMP;
    }

    @Exclude
    public long getTimestamp() {
        return mTimestamp;
    }

    @Keep
    @RestrictTo(RestrictTo.Scope.TESTS)
    public void setTimestamp(long time) {
        mTimestamp = time;
    }

    public void add(Context context) {
        DatabaseReference index = getIndicesRef().push();
        mKey = index.getKey();
        Long number = getNumberAsLong();
        index.setValue(number, number);
        mTemplateKey = context.getSharedPreferences(SCOUT_TEMPLATE, Context.MODE_PRIVATE)
                .getString(SCOUT_TEMPLATE, null);
        forceUpdate();
        FirebaseUserActions.getInstance()
                .end(new Action.Builder(Action.Builder.ADD_ACTION)
                             .setObject(getFormattedName(), getDeepLink())
                             .build());
    }

    public void update(Team newTeam) {
        if (equals(newTeam)) {
            getRef().child(FIREBASE_TIMESTAMP).setValue(getServerTimestamp());
            return;
        }

        checkForMatchingDetails(newTeam);
        if (!mHasCustomName) mName = newTeam.getName();
        if (!mHasCustomMedia) mMedia = newTeam.getMedia();
        if (!mHasCustomWebsite) mWebsite = newTeam.getWebsite();
        forceUpdate();
    }

    public void forceUpdate() {
        getRef().setValue(this);
        FirebaseAppIndex.getInstance().update(getIndexable());
    }

    private void checkForMatchingDetails(Team newTeam) {
        if (mHasCustomName && getName().equals(newTeam.getName())) {
            mHasCustomName = false;
        }
        if (mHasCustomMedia && getMedia().equals(newTeam.getMedia())) {
            mHasCustomMedia = false;
        }
        if (mHasCustomWebsite && getWebsite().equals(newTeam.getWebsite())) {
            mHasCustomWebsite = false;
        }
    }

    public void updateTemplateKey(final String key, Context context) {
        mTemplateKey = key;
        getRef().child(FIREBASE_TEMPLATE_KEY).setValue(mTemplateKey);
        context.getSharedPreferences(SCOUT_TEMPLATE, Context.MODE_PRIVATE)
                .edit()
                .putString(SCOUT_TEMPLATE, key)
                .apply();
        TeamIndices.getAll().addOnSuccessListener(new OnSuccessListener<List<DataSnapshot>>() {
            private final ValueEventListener TEAM_TEMPLATE_UPDATER = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    DataSnapshot templateSnapshot = snapshot.child(FIREBASE_TEMPLATE_KEY);
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
                            .addListenerForSingleValueEvent(TEAM_TEMPLATE_UPDATER);
                }
            }
        });
    }

    public void delete(Context context) {
        final Context appContext = context.getApplicationContext();
        Scout.deleteAll(getKey()).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                getRef().removeValue();
                getIndicesRef().child(getKey()).removeValue();
                if (getTemplateKey() != null) {
                    TeamIndices.getAll()
                            .addOnSuccessListener(new OnSuccessListener<List<DataSnapshot>>() {
                                @Override
                                public void onSuccess(List<DataSnapshot> snapshots) {
                                    if (snapshots.isEmpty()) {
                                        Constants.FIREBASE_SCOUT_TEMPLATES.child(getTemplateKey())
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
        long differenceDays = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - mTimestamp);
        if (differenceDays >= FRESHNESS_DAYS) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                DownloadTeamDataJob21.start(this, context);
            } else {
                DownloadTeamDataJob.start(this);
            }
        }
    }

    public void visitTbaWebsite(Context context) {
        Uri tbaUrl = Uri.parse("https://www.thebluealliance.com/team/" + getNumber());
        BaseHelper.launchUrl(context, tbaUrl);
    }

    public void visitTeamWebsite(Context context) {
        BaseHelper.launchUrl(context, Uri.parse(getWebsite()));
    }

    @Exclude
    public Indexable getIndexable() {
        DigitalDocumentBuilder builder = Indexables.digitalDocumentBuilder()
                .setUrl(getDeepLink())
                .setName(getFormattedName())
                .setMetadata(new Indexable.Metadata.Builder().setWorksOffline(true));
        if (getMedia() != null) builder.setImage(getMedia());
        return builder.build();
    }

    @Exclude
    public String getDeepLink() {
        return TeamReceiver.APP_LINK_BASE + getLinkKeyNumberPair();
    }

    @Exclude
    public String getLinkKeyNumberPair() {
        return "&" + TeamReceiver.TEAM_QUERY_KEY + "=" + getKey() + ":" + getNumber();
    }

    @Exclude
    public Action getViewAction() {
        return new Action.Builder(Action.Builder.VIEW_ACTION)
                .setObject(getFormattedName(), getDeepLink())
                .build();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(Preconditions.checkNotNull(mNumber, "Team number cannot be null."));
        dest.writeString(mKey);
        dest.writeString(mTemplateKey);
        dest.writeString(mName);
        dest.writeString(mMedia);
        dest.writeString(mWebsite);
        dest.writeInt(getIntForBoolean(mHasCustomName));
        dest.writeInt(getIntForBoolean(mHasCustomMedia));
        dest.writeInt(getIntForBoolean(mHasCustomWebsite));
        dest.writeLong(mTimestamp);
    }

    private int getIntForBoolean(boolean value) {
        return value ? 1 : 0;
    }

    @SuppressWarnings("PMD.UselessParentheses")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Team team = (Team) o;

        return mHasCustomName == team.mHasCustomName
                && mHasCustomMedia == team.mHasCustomMedia
                && mHasCustomWebsite == team.mHasCustomWebsite
                && mTimestamp == team.mTimestamp
                && (mNumber == null ? team.mNumber == null : mNumber.equals(team.mNumber))
                && (mKey == null ? team.mKey == null : mKey.equals(team.mKey))
                && (mTemplateKey == null ? team.mTemplateKey == null : mTemplateKey.equals(team.mTemplateKey))
                && (mName == null ? team.mName == null : mName.equals(team.mName))
                && (mMedia == null ? team.mMedia == null : mMedia.equals(team.mMedia))
                && (mWebsite == null ? team.mWebsite == null : mWebsite.equals(team.mWebsite));
    }

    @Override
    public int hashCode() {
        int result = mNumber.hashCode();
        result = 31 * result + (mKey == null ? 0 : mKey.hashCode());
        result = 31 * result + (mTemplateKey == null ? 0 : mTemplateKey.hashCode());
        result = 31 * result + (mName == null ? 0 : mName.hashCode());
        result = 31 * result + (mMedia == null ? 0 : mMedia.hashCode());
        result = 31 * result + (mWebsite == null ? 0 : mWebsite.hashCode());
        result = 31 * result + (mHasCustomName ? 1 : 0);
        result = 31 * result + (mHasCustomMedia ? 1 : 0);
        result = 31 * result + (mHasCustomWebsite ? 1 : 0);
        result = 31 * result + (int) (mTimestamp ^ (mTimestamp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "Team " + mNumber + " last updated " +
                TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - mTimestamp) +
                " day(s) ago{" +
                ", mKey='" + mKey + '\'' +
                ", mTemplateKey='" + mTemplateKey + '\'' +
                ", mName='" + mName + '\'' +
                ", mMedia='" + mMedia + '\'' +
                ", mWebsite='" + mWebsite + '\'' +
                ", mHasCustomName=" + mHasCustomName +
                ", mHasCustomMedia=" + mHasCustomMedia +
                ", mHasCustomWebsite=" + mHasCustomWebsite +
                ", mTimestamp=" + mTimestamp +
                '}';
    }

    @Override
    public int compareTo(Team team) {
        long number = getNumberAsLong();
        long otherTeamNumber = team.getNumberAsLong();
        if (number > otherTeamNumber) {
            return 1;
        } else if (number == otherTeamNumber) {
            return 0;
        } else {
            return -1;
        }
    }

    public static class Builder implements com.supercilex.robotscouter.data.util.Builder<Team> {
        private final String mNumber;
        private String mKey;
        private String mTemplateKey;
        private String mName;
        private String mMedia;
        private String mWebsite;
        private boolean mHasCustomName;
        private boolean mHasCustomMedia;
        private boolean mHasCustomWebsite;
        private long mTimestamp;

        public Builder(@NonNull String number) {
            mNumber = number;
        }

        public Builder(@NonNull Team team) {
            mNumber = team.getNumber();
            mKey = team.getKey();
            mTemplateKey = team.getTemplateKey();
            mName = team.getName();
            mMedia = team.getMedia();
            mWebsite = team.getWebsite();
            if (team.getHasCustomName() != null) mHasCustomName = team.getHasCustomName();
            if (team.getHasCustomMedia() != null) mHasCustomMedia = team.getHasCustomMedia();
            if (team.getHasCustomWebsite() != null) mHasCustomWebsite = team.getHasCustomWebsite();
            mTimestamp = team.getTimestamp();
        }

        public Builder setKey(String key) {
            mKey = key;
            return this;
        }

        public Builder setTemplateKey(String templateKey) {
            mTemplateKey = templateKey;
            return this;
        }

        public Builder setName(String name) {
            mName = name;
            return this;
        }

        public Builder setMedia(String media) {
            mMedia = media;
            return this;
        }

        public Builder setWebsite(String website) {
            mWebsite = website;
            return this;
        }

        public Builder setHasCustomName(boolean hasCustomName) {
            mHasCustomName = hasCustomName;
            return this;
        }

        public Builder setHasCustomMedia(boolean hasCustomMedia) {
            mHasCustomMedia = hasCustomMedia;
            return this;
        }

        public Builder setHasCustomWebsite(boolean hasCustomWebsite) {
            mHasCustomWebsite = hasCustomWebsite;
            return this;
        }

        public Builder setTimestamp(long timestamp) {
            mTimestamp = timestamp;
            return this;
        }

        @Override
        public Team build() {
            return new Team(mNumber,
                            mKey,
                            mTemplateKey,
                            mName,
                            mMedia,
                            mWebsite,
                            mHasCustomName,
                            mHasCustomMedia,
                            mHasCustomWebsite,
                            mTimestamp);
        }
    }
}

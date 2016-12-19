package com.supercilex.robotscouter.data.model;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.PropertyName;
import com.google.firebase.database.ServerValue;
import com.supercilex.robotscouter.data.job.DownloadTeamDataJob;
import com.supercilex.robotscouter.util.BaseHelper;
import com.supercilex.robotscouter.util.Constants;
import com.supercilex.robotscouter.util.Preconditions;

import java.util.concurrent.TimeUnit;

public class Team implements Parcelable {
    public static final Creator<Team> CREATOR = new Creator<Team>() {
        @Override
        public Team createFromParcel(Parcel in) {
            return new Builder(in.readString())
                    .setKey(in.readString())
                    .setTemplateKey(in.readString())
                    .setName(in.readString())
                    .setMedia(in.readString())
                    .setWebsite(in.readString())
                    .setHasCustomName(getBooleanForInt(in.readInt()))
                    .setHasCustomMedia(getBooleanForInt(in.readInt()))
                    .setHasCustomWebsite(getBooleanForInt(in.readInt()))
                    .setTimestamp(in.readLong())
                    .build();
        }

        @Override
        public Team[] newArray(int size) {
            return new Team[size];
        }

        private boolean getBooleanForInt(int value) {
            return value == 1;
        }
    };

    private static final int WEEK = 7;

    private String mNumber;
    private String mKey;
    private String mTemplateKey;
    private String mName;
    private String mMedia;
    private String mWebsite;
    private boolean mHasCustomName;
    private boolean mHasCustomWebsite;
    private boolean mHasCustomMedia;
    private long mTimestamp;
    private boolean mShouldUpdateTimestamp = true;

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
                 boolean hasCustomWebsite,
                 boolean hasCustomMedia,
                 long timestamp) {
        mNumber = number;
        mKey = key;
        mTemplateKey = templateKey;
        mName = name;
        mMedia = media;
        mWebsite = website;
        mHasCustomName = hasCustomName;
        mHasCustomWebsite = hasCustomWebsite;
        mHasCustomMedia = hasCustomMedia;
        mTimestamp = timestamp;
    }

    @Exclude
    public static DatabaseReference getIndicesRef() {
        return Constants.FIREBASE_TEAM_INDICES.child(BaseHelper.getUid());
    }

    @Exclude
    public static Team getTeam(Intent intent) {
        return (Team) Preconditions.checkNotNull(intent.getParcelableExtra(Constants.INTENT_TEAM),
                                                 "Team cannot be null");
    }

    @Exclude
    public static Team getTeam(Bundle arguments) {
        return (Team) Preconditions.checkNotNull(arguments.getParcelable(Constants.INTENT_TEAM),
                                                 "Team cannot be null");
    }

    @Exclude
    public Intent getIntent() {
        return new Intent().putExtra(Constants.INTENT_TEAM, this);
    }

    @Exclude
    public Bundle getBundle() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(Constants.INTENT_TEAM, this);
        return bundle;
    }

    @Exclude
    public DatabaseReference getRef() {
        return Constants.FIREBASE_TEAMS.child(mKey);
    }

    @Keep
    public String getNumber() {
        return mNumber;
    }

    @Keep
    public void setNumber(String number) {
        if (mNumber != null) throw new IllegalStateException("Team number cannot be changed");
        mNumber = number;
    }

    @Exclude
    public String getKey() {
        return mKey;
    }

    @Exclude
    public void setKey(String key) {
        if (mKey != null) throw new IllegalStateException("Team key cannot be changed");
        mKey = key;
    }

    @Keep
    public String getTemplateKey() {
        return mTemplateKey;
    }

    @Keep
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
    public void setHasCustomName(boolean hasCustomName) {
        mHasCustomName = hasCustomName;
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
    public void setHasCustomMedia(boolean hasCustomMedia) {
        mHasCustomMedia = hasCustomMedia;
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
    public void setHasCustomWebsite(boolean hasCustomWebsite) {
        mHasCustomWebsite = hasCustomWebsite;
    }

    @Keep
    @PropertyName(Constants.FIREBASE_TIMESTAMP)
    public Object getServerValue() {
        if (mShouldUpdateTimestamp) {
            return ServerValue.TIMESTAMP;
        } else {
            return mTimestamp;
        }
    }

    @Exclude
    public long getTimestamp() {
        return mTimestamp;
    }

    @Keep
    public void setTimestamp(long time) {
        mTimestamp = time;
    }

    public void add() {
        DatabaseReference index = getIndicesRef().push();
        mKey = index.getKey();
        Long number = Long.valueOf(mNumber);
        index.setValue(number, number);
        forceUpdate();
    }

    public void update(Team newTeam) {
        if (!mHasCustomName) mName = newTeam.getName();
        if (!mHasCustomWebsite) mWebsite = newTeam.getWebsite();
        if (!mHasCustomMedia) mMedia = newTeam.getMedia();
        getRef().setValue(this);
    }

    public void forceUpdate() {
        mShouldUpdateTimestamp = false;
        getRef().setValue(this);
        mShouldUpdateTimestamp = true;
    }

    public void updateTemplateKey(String key) {
        mTemplateKey = key;
        getRef().child(Constants.FIREBASE_TEMPLATE_KEY).setValue(mTemplateKey);
    }

    public void fetchLatestData() {
        long differenceDays = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - mTimestamp);
        if (differenceDays >= WEEK) DownloadTeamDataJob.start(this);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(Preconditions.checkNotNull(mNumber, "Team number cannot be null."));
        parcel.writeString(mKey);
        parcel.writeString(mTemplateKey);
        parcel.writeString(mName);
        parcel.writeString(mMedia);
        parcel.writeString(mWebsite);
        parcel.writeInt(getIntForBoolean(mHasCustomName));
        parcel.writeInt(getIntForBoolean(mHasCustomMedia));
        parcel.writeInt(getIntForBoolean(mHasCustomWebsite));
        parcel.writeLong(mTimestamp);
    }

    private int getIntForBoolean(boolean value) {
        return value ? 1 : 0;
    }

    @Override
    public String toString() {
        return "Team " + mNumber + " last updated " +
                TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - mTimestamp) +
                " day(s) ago:\n" +
                "Key: " + mKey + "\n" +
                "Scout template key: " + mTemplateKey + "\n" +
                "Name: " + mName + "\n" +
                "Media: " + mMedia + "\n" +
                "Website: " + mWebsite + "\n" +
                "Has custom name: " + mHasCustomName + "\n" +
                "Has custom media: " + mHasCustomMedia + "\n" +
                "Has custom website: " + mHasCustomWebsite + "\n" +
                "Should update timestamp: " + mShouldUpdateTimestamp;
    }

    public static final class Builder {
        private final String mNumber;
        private String mKey;
        private String mTemplateKey;
        private String mName;
        private String mMedia;
        private String mWebsite;
        private boolean mHasCustomName;
        private boolean mHasCustomWebsite;
        private boolean mHasCustomMedia;
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
            if (team.getHasCustomWebsite() != null) mHasCustomWebsite = team.getHasCustomWebsite();
            if (team.getHasCustomMedia() != null) mHasCustomMedia = team.getHasCustomMedia();
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

        public Builder setHasCustomWebsite(boolean hasCustomWebsite) {
            mHasCustomWebsite = hasCustomWebsite;
            return this;
        }

        public Builder setHasCustomMedia(boolean hasCustomMedia) {
            mHasCustomMedia = hasCustomMedia;
            return this;
        }

        public Builder setTimestamp(long timestamp) {
            mTimestamp = timestamp;
            return this;
        }

        public Team build() {
            return new Team(mNumber,
                            mKey,
                            mTemplateKey,
                            mName,
                            mMedia,
                            mWebsite,
                            mHasCustomName,
                            mHasCustomWebsite,
                            mHasCustomMedia, mTimestamp);
        }
    }
}

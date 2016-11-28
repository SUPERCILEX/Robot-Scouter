package com.supercilex.robotscouter.data.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.PropertyName;
import com.google.firebase.database.ServerValue;
import com.supercilex.robotscouter.data.job.DownloadTeamDataJob;
import com.supercilex.robotscouter.util.BaseHelper;
import com.supercilex.robotscouter.util.Constants;
import com.supercilex.robotscouter.util.Preconditions;

public class Team implements Parcelable {
    private static final int WEEK = 7;

    private String mKey;
    private String mNumber;
    private String mName;
    private String mMedia;
    private String mWebsite;
    private Boolean mHasCustomName;
    private Boolean mHasCustomWebsite;
    private Boolean mHasCustomMedia;
    private long mTimestamp;
    private boolean mShouldUpdateTimestamp = true;

    public Team() {
        // Needed for Firebase
    }

    public Team(@NonNull String number) {
        mNumber = number;
    }

    public Team(Team team) {
        mKey = team.getKey();
        mNumber = team.getNumber();
        mName = team.getName();
        mMedia = team.getMedia();
        mWebsite = team.getWebsite();
        mHasCustomName = team.getHasCustomName();
        mHasCustomWebsite = team.getHasCustomWebsite();
        mHasCustomMedia = team.getHasCustomMedia();
        mTimestamp = team.getTimestamp();
    }

    @Exclude
    public static DatabaseReference getIndicesRef() {
        return BaseHelper.getDatabase()
                .child(Constants.FIREBASE_TEAM_INDICES)
                .child(BaseHelper.getUid());
    }

    @Exclude
    public DatabaseReference getRef() {
        return BaseHelper.getDatabase().child(Constants.FIREBASE_TEAMS).child(mKey);
    }

    @Keep
    public String getNumber() {
        return mNumber;
    }

    @Keep
    public void setNumber(String number) {
        mNumber = number;
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
    public String getWebsite() {
        return mWebsite;
    }

    @Keep
    public void setWebsite(String website) {
        mWebsite = website;
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
    @PropertyName(Constants.FIREBASE_CUSTOM_NAME)
    public Boolean getHasCustomName() {
        return mHasCustomName;
    }

    @Keep
    @PropertyName(Constants.FIREBASE_CUSTOM_NAME)
    public void setHasCustomName(boolean hasCustomName) {
        mHasCustomName = hasCustomName;
    }

    @Keep
    @PropertyName(Constants.FIREBASE_CUSTOM_WEBSITE)
    public Boolean getHasCustomWebsite() {
        return mHasCustomWebsite;
    }

    @Keep
    @PropertyName(Constants.FIREBASE_CUSTOM_WEBSITE)
    public void setHasCustomWebsite(boolean hasCustomWebsite) {
        mHasCustomWebsite = hasCustomWebsite;
    }

    @Keep
    @PropertyName(Constants.FIREBASE_CUSTOM_MEDIA)
    public Boolean getHasCustomMedia() {
        return mHasCustomMedia;
    }

    @Keep
    @PropertyName(Constants.FIREBASE_CUSTOM_MEDIA)
    public void setHasCustomMedia(boolean hasCustomMedia) {
        mHasCustomMedia = hasCustomMedia;
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

    @Exclude
    public String getKey() {
        return mKey;
    }

    @Exclude
    public void setKey(String key) {
        mKey = key;
    }

    public void add() {
        DatabaseReference index = getIndicesRef().push();
        mKey = index.getKey();
        Long number = Long.valueOf(mNumber);
        index.setValue(number, number);
        forceUpdate();
    }

    public void update(Team newTeam) {
        if (mHasCustomName == null) {
            mName = newTeam.getName();
        }
        if (mHasCustomWebsite == null) {
            mWebsite = newTeam.getWebsite();
        }
        if (mHasCustomMedia == null) {
            mMedia = newTeam.getMedia();
        }
        getRef().setValue(this);
    }

    public void forceUpdate() {
        mShouldUpdateTimestamp = false;
        getRef().setValue(this);
        mShouldUpdateTimestamp = true;
    }

    public void fetchLatestData() {
        long differenceDays = (System.currentTimeMillis() - mTimestamp) / (1000 * 60 * 60 * 24);
        if (differenceDays >= WEEK) {
            DownloadTeamDataJob.start(this);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(mKey);
        parcel.writeString(Preconditions.checkNotNull(mNumber, "Team number cannot be null."));
        parcel.writeString(mName);
        parcel.writeString(mMedia);
        parcel.writeString(mWebsite);
        parcel.writeInt(mHasCustomName == null ? 0 : 1);
        parcel.writeInt(mHasCustomWebsite == null ? 0 : 1);
        parcel.writeInt(mHasCustomMedia == null ? 0 : 1);
        parcel.writeLong(mTimestamp);
    }

    public static final Creator<Team> CREATOR = new Creator<Team>() {
        @Override
        public Team createFromParcel(Parcel in) {
            return new Team(in);
        }

        @Override
        public Team[] newArray(int size) {
            return new Team[size];
        }
    };

    private Team(Parcel in) {
        mKey = in.readString();
        mNumber = in.readString();
        mName = in.readString();
        mMedia = in.readString();
        mWebsite = in.readString();
        mHasCustomName = trueOrNull(in.readInt());
        mHasCustomWebsite = trueOrNull(in.readInt());
        mHasCustomMedia = trueOrNull(in.readInt());
        mTimestamp = in.readLong();
    }

    private Boolean trueOrNull(int in) {
        if (in != 0) {
            return true;
        } else {
            return null;
        }
    }
}

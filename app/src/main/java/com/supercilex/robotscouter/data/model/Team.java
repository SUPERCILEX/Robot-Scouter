package com.supercilex.robotscouter.data.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.PropertyName;
import com.google.firebase.database.ServerValue;
import com.supercilex.robotscouter.util.BaseHelper;
import com.supercilex.robotscouter.util.Constants;
import com.supercilex.robotscouter.util.Preconditions;

public class Team implements Parcelable {
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
    }

    public Team(@NonNull String number, String key) {
        mKey = key;
        mNumber = number;
    }

    public Team(String teamName, String teamWebsite, String teamLogoUrl) {
        mName = teamName;
        mMedia = teamLogoUrl;
        mWebsite = teamWebsite;
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
        DatabaseReference ref = BaseHelper.getDatabase();
        mKey = ref.push().getKey();
        ref.child(Constants.FIREBASE_TEAM_INDEXES)
                .child(BaseHelper.getUid())
                .child(mKey)
                .setValue(mNumber, Long.valueOf(mNumber));
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
        getTeamRef().setValue(this);
    }

    public void forceUpdate() {
        mShouldUpdateTimestamp = false;
        getTeamRef().setValue(this);
        mShouldUpdateTimestamp = true;
    }

    @Exclude
    public DatabaseReference getTeamRef() {
        return BaseHelper.getDatabase().child(Constants.FIREBASE_TEAMS).child(mKey);
    }

    public void fetchLatestData() {
        long differenceDays = (System.currentTimeMillis() - mTimestamp) / (1000 * 60 * 60 * 24);
        if (differenceDays >= 7) {
//            DownloadTeamDataJob.start(this);
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
        parcel.writeInt(mHasCustomName != null ? 1 : 0);
        parcel.writeInt(mHasCustomWebsite != null ? 1 : 0);
        parcel.writeInt(mHasCustomMedia != null ? 1 : 0);
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

package com.supercilex.robotscouter.data.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;

import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.PropertyName;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.supercilex.robotscouter.data.job.DownloadTeamDataJob;
import com.supercilex.robotscouter.util.BaseHelper;
import com.supercilex.robotscouter.util.Constants;
import com.supercilex.robotscouter.util.Preconditions;

import java.util.Map;

public class Team implements Parcelable {
    private String mKey;

    private String mNumber;
    private String mName;
    private String mMedia;
    private String mWebsite;
    private String mCustomName;
    private String mCustomWebsite;
    private String mCustomMedia;
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
    public String getCustomName() {
        return mCustomName;
    }

    @Keep
    @PropertyName(Constants.FIREBASE_CUSTOM_NAME)
    public void setCustomName(String customName) {
        mCustomName = customName;
    }

    @Keep
    @PropertyName(Constants.FIREBASE_CUSTOM_WEBSITE)
    public String getCustomWebsite() {
        return mCustomWebsite;
    }

    @Keep
    @PropertyName(Constants.FIREBASE_CUSTOM_WEBSITE)
    public void setCustomWebsite(String customWebsite) {
        mCustomWebsite = customWebsite;
    }

    @Keep
    @PropertyName(Constants.FIREBASE_CUSTOM_MEDIA)
    public String getCustomMedia() {
        return mCustomMedia;
    }

    @Keep
    @PropertyName(Constants.FIREBASE_CUSTOM_MEDIA)
    public void setCustomMedia(String customMedia) {
        mCustomMedia = customMedia;
    }

    // TODO: 11/09/2016 test this to make sure it doesn't delete timestamp
    // might have to make it return object
    @Keep
    @PropertyName(Constants.FIREBASE_TIMESTAMP)
    public Map<String, String> getServerValue() {
        if (mShouldUpdateTimestamp) {
            return ServerValue.TIMESTAMP;
        } else {
            return null;
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
        mShouldUpdateTimestamp = false;

        DatabaseReference ref = BaseHelper.getDatabase();
        mKey = ref.push().getKey();
        ref.child(Constants.FIREBASE_TEAM_INDEXES)
                .child(BaseHelper.getUid())
                .child(mKey)
                .setValue(mNumber, Long.valueOf(mNumber));
        update();

        mShouldUpdateTimestamp = true;
    }

    public void update() {
        if (getCustomName() == null) {
            setName(mName);
        }
        if (getCustomWebsite() == null) {
            setWebsite(mWebsite);
        }
        if (getCustomMedia() == null) {
            setMedia(mMedia);
        }
        getTeamRef().setValue(this);
    }

    // TODO: 11/09/2016 remove
    public void updateWithCustomDetails(@NonNull String teamNumber, @NonNull String key) {
        mNumber = teamNumber;
        DatabaseReference ref = BaseHelper.getDatabase()
                .child(Constants.FIREBASE_TEAMS)
                .child(key);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Team team = dataSnapshot.getValue(Team.class);

                if (!team.getName().equals(mName)) {
                    dataSnapshot.getRef().child(Constants.FIREBASE_CUSTOM_NAME).setValue(true);
                }

                if (!team.getWebsite().equals(mWebsite)) {
                    dataSnapshot.getRef().child(Constants.FIREBASE_CUSTOM_WEBSITE).setValue(true);
                }

                if (!team.getMedia().equals(mMedia)) {
                    dataSnapshot.getRef().child(Constants.FIREBASE_CUSTOM_MEDIA).setValue(true);
                }

                mShouldUpdateTimestamp = false;
                dataSnapshot.getRef().setValue(Team.this);
                mShouldUpdateTimestamp = true;
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                FirebaseCrash.report(databaseError.toException());
            }
        });
    }

    @Exclude
    public DatabaseReference getTeamRef() {
        return BaseHelper.getDatabase().child(Constants.FIREBASE_TEAMS).child(mKey);
    }

    public void fetchLatestData() {
        long differenceDays = (System.currentTimeMillis() - mTimestamp) / (1000 * 60 * 60 * 24);
        if (differenceDays >= 7) {
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
        parcel.writeString(mCustomName);
        parcel.writeString(mCustomWebsite);
        parcel.writeString(mCustomMedia);
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
        mCustomName = in.readString();
        mCustomWebsite = in.readString();
        mCustomMedia = in.readString();
        mTimestamp = in.readLong();
    }
}

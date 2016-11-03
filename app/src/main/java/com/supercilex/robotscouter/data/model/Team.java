package com.supercilex.robotscouter.data.model;

import android.app.Activity;
import android.support.annotation.NonNull;

import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.PropertyName;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.supercilex.robotscouter.util.Constants;
import com.supercilex.robotscouter.util.FirebaseUtils;

import java.util.Map;

public class Team {
    private String mKey;

    private String mNumber;
    private String mName;
    private String mMedia;
    private String mWebsite;
    private long mTimestamp;
    private boolean mShouldUpdateTimestamp = true;

    public Team() {
    }

    public Team(String key, String number) {
        mKey = key;
        mNumber = number;
    }

    public Team(String teamName, String teamWebsite, String teamLogoUrl) {
        mName = teamName;
        mMedia = teamLogoUrl;
        mWebsite = teamWebsite;
    }

    public String getNumber() {
        return mNumber;
    }

    public void setNumber(String number) {
        mNumber = number;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getMedia() {
        return mMedia;
    }

    public void setMedia(String media) {
        mMedia = media;
    }

    public String getWebsite() {
        return mWebsite;
    }

    public void setWebsite(String website) {
        mWebsite = website;
    }

    @PropertyName("timestamp")
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

        String userId = FirebaseUtils.getUid();
        DatabaseReference ref = FirebaseUtils.getDatabase();
        String key = ref.push().getKey();

        ref.child(Constants.FIREBASE_TEAM_INDEXES)
                .child(userId)
                .child(key)
                .setValue(mNumber, Long.valueOf(mNumber));

        ref.child(Constants.FIREBASE_TEAMS).child(key).setValue(this);

        mShouldUpdateTimestamp = true;
        mKey = key;
    }

    public void overwriteData() {
        FirebaseUtils.getDatabase().child(Constants.FIREBASE_TEAMS).child(mKey).setValue(this);
    }

    public void update(@NonNull String teamNumber, @NonNull String key) {
        mNumber = teamNumber;
        DatabaseReference ref = FirebaseUtils.getDatabase()
                .child(Constants.FIREBASE_TEAMS)
                .child(key);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null) {
                    if (dataSnapshot.child(Constants.FIREBASE_CUSTOM_NAME).getValue() == null) {
                        dataSnapshot.getRef().child(Constants.FIREBASE_NAME).setValue(mName);
                    }

                    if (dataSnapshot.child(Constants.FIREBASE_CUSTOM_WEBSITE).getValue() == null) {
                        dataSnapshot.getRef()
                                .child(Constants.FIREBASE_WEBSITE)
                                .setValue(mWebsite);
                    }

                    if (dataSnapshot.child(Constants.FIREBASE_CUSTOM_MEDIA).getValue() == null) {
                        dataSnapshot.getRef().child(Constants.FIREBASE_MEDIA).setValue(mMedia);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                FirebaseCrash.report(databaseError.toException());
            }
        });

        ref.child(Constants.FIREBASE_TIMESTAMP).setValue(getServerValue());
    }

    public void updateWithCustomDetails(@NonNull String teamNumber, @NonNull String key) {
        mNumber = teamNumber;
        DatabaseReference ref = FirebaseUtils.getDatabase()
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

    public void fetchLatestData(Activity activity, String key) {
//        long differenceDays = (System.currentTimeMillis() - getLastUpdatedLong()) / (1000 * 60 * 60 * 24);
//
//        if (differenceDays >= 7) {
//            Driver myDriver = new GooglePlayDriver(activity);
//            FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(myDriver);
//
//            Bundle bundle = new Bundle();
//            bundle.putString(Constants.INTENT_TEAM_NUMBER, mNumber);
//            bundle.putString(Constants.INTENT_TEAM_KEY, key);
//
//            Job job = dispatcher.newJobBuilder()
//                    .setService(DownloadTeamDataJob.class)
//                    .setTag(mNumber)
//                    .setReplaceCurrent(true)
//                    .setConstraints(Constraint.ON_ANY_NETWORK)
//                    .setTrigger(Trigger.NOW)
//                    .setExtras(bundle)
//                    .build();
//
//            int result = dispatcher.schedule(job);
//            if (result != FirebaseJobDispatcher.SCHEDULE_RESULT_SUCCESS) {
//                FirebaseCrash.report(new IllegalArgumentException("Job Scheduler failed."));
//            }
//        }
    }
}

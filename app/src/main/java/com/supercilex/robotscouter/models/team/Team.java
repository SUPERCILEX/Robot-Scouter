package com.supercilex.robotscouter.models.team;

import android.app.Activity;
import android.support.annotation.NonNull;

import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.supercilex.robotscouter.Constants;
import com.supercilex.robotscouter.Utils;

public class Team {
    private String mNumber;
    private String mName;
    private String mMedia;
    private String mWebsite;
    private long mLastUpdated;
    private boolean mNewTeam = false;

    public Team() {
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

    public java.util.Map<String, String> getLastUpdated() {
        if (!mNewTeam) {
            return ServerValue.TIMESTAMP;
        } else {
            return null;
        }
    }

    @SuppressWarnings("unused") // Used for Firebase
    public void setLastUpdated(long time) {
        mLastUpdated = time;
    }

    public String addTeam(@NonNull String teamNumber) {
        mNumber = teamNumber;
        mNewTeam = true;

        String userId = Utils.getUser().getUid();
        DatabaseReference ref = Utils.getDatabase().getReference();
        String key = ref.push().getKey();

        ref.child(Constants.FIREBASE_TEAM_INDEXES)
                .child(userId)
                .child(key)
                .setValue(Long.valueOf(mNumber)); // Need to use long for orderByValue() in MainActivity

        ref.child(Constants.FIREBASE_TEAMS).child(key).setValue(this);

        mNewTeam = false;
        return key;
    }

    public void addTeamData(@NonNull String key) {
        Utils.getDatabase()
                .getReference()
                .child(Constants.FIREBASE_TEAMS)
                .child(key)
                .setValue(this);
    }

    public void updateTeam(@NonNull String teamNumber, @NonNull String key) {
        mNumber = teamNumber;
        DatabaseReference ref = Utils.getDatabase()
                .getReference()
                .child(Constants.FIREBASE_TEAMS)
                .child(key);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null) {
                    if (dataSnapshot.child(Constants.FIREBASE_CUSTOM_NAME).getValue() == null) {
                        dataSnapshot.getRef().child(Constants.FIREBASE_TEAM_NAME).setValue(mName);
                    }

                    if (dataSnapshot.child(Constants.FIREBASE_CUSTOM_WEBSITE).getValue() == null) {
                        dataSnapshot.getRef()
                                .child(Constants.FIREBASE_TEAM_WEBSITE)
                                .setValue(mWebsite);
                    }

                    if (dataSnapshot.child(Constants.FIREBASE_CUSTOM_MEDIA).getValue() == null) {
                        dataSnapshot.getRef().child(Constants.FIREBASE_TEAM_MEDIA).setValue(mMedia);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                FirebaseCrash.report(databaseError.toException());
            }
        });

        ref.child(Constants.FIREBASE_LAST_UPDATED).setValue(ServerValue.TIMESTAMP);
    }

    public void updateTeamOverwrite(@NonNull String teamNumber, @NonNull String key) {
        mNumber = teamNumber;
        DatabaseReference ref = Utils.getDatabase()
                .getReference()
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

                dataSnapshot.getRef().setValue(Team.this);

                dataSnapshot.getRef()
                        .child(Constants.FIREBASE_LAST_UPDATED)
                        .setValue(dataSnapshot.child(Constants.FIREBASE_LAST_UPDATED).getValue());
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

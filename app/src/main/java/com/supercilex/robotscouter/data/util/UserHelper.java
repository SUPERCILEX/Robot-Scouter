package com.supercilex.robotscouter.data.util;

import android.text.TextUtils;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.supercilex.robotscouter.data.model.User;
import com.supercilex.robotscouter.ui.AuthHelper;
import com.supercilex.robotscouter.util.Constants;

public class UserHelper {
    private final User mUser;

    public UserHelper(User user) {
        mUser = user;
    }

    public static DatabaseReference getScoutTemplateIndicesRef() {
        return getScoutTemplateIndicesRef(AuthHelper.getUid());
    }

    private static DatabaseReference getScoutTemplateIndicesRef(String uid) {
        return Constants.FIREBASE_USERS.child(uid).child(Constants.SCOUT_TEMPLATE_INDICES);
    }

    public void add() {
        Constants.FIREBASE_USERS.child(mUser.getUid()).setValue(mUser);
    }

    public void transferData(String prevUid) {
        if (TextUtils.isEmpty(prevUid)) return;

        final DatabaseReference prevTeamRef = Constants.FIREBASE_TEAM_INDICES.child(prevUid);
        new FirebaseCopier(prevTeamRef, TeamHelper.getIndicesRef()) {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                super.onDataChange(snapshot);
                prevTeamRef.removeValue();
            }
        }.performTransformation();

        final DatabaseReference prevScoutTemplatesRef = getScoutTemplateIndicesRef(prevUid);
        new FirebaseCopier(prevScoutTemplatesRef, getScoutTemplateIndicesRef()) {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                super.onDataChange(snapshot);
                prevScoutTemplatesRef.removeValue();
            }
        }.performTransformation();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserHelper helper = (UserHelper) o;

        return mUser.equals(helper.mUser);
    }

    @Override
    public int hashCode() {
        return mUser.hashCode();
    }

    @Override
    public String toString() {
        return mUser.toString();
    }
}

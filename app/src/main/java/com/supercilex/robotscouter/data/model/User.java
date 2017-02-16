package com.supercilex.robotscouter.data.model;

import android.net.Uri;
import android.support.annotation.Keep;
import android.text.TextUtils;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;
import com.supercilex.robotscouter.data.util.FirebaseCopier;
import com.supercilex.robotscouter.data.util.TeamHelper;
import com.supercilex.robotscouter.util.Constants;

public class User {
    @Exclude private final String mUid;
    @Exclude private String mEmail;
    @Exclude private String mName;
    @Exclude private Uri mPhotoUrl;

    private User(String uid, String email, String name, Uri photoUrl) {
        mUid = uid;
        mEmail = email;
        mName = name;
        mPhotoUrl = photoUrl;
    }

    @Keep
    public String getEmail() {
        return mEmail;
    }

    @Keep
    public void setEmail(String email) {
        mEmail = email;
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
    public String getPhotoUrl() {
        return mPhotoUrl == null ? null : mPhotoUrl.toString();
    }

    @Keep
    public void setPhotoUrl(Uri photoUrl) {
        mPhotoUrl = photoUrl;
    }

    public void add() {
        Constants.FIREBASE_USERS.child(mUid).setValue(this);
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
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        return TextUtils.equals(mUid, user.mUid)
                && TextUtils.equals(mEmail, user.mEmail)
                && TextUtils.equals(mName, user.mName)
                && (mPhotoUrl == null ? user.mPhotoUrl == null : mPhotoUrl.equals(user.mPhotoUrl));
    }

    @Override
    public int hashCode() {
        int result = mUid.hashCode();
        result = 31 * result + (mEmail == null ? 0 : mEmail.hashCode());
        result = 31 * result + (mName == null ? 0 : mName.hashCode());
        result = 31 * result + (mPhotoUrl == null ? 0 : mPhotoUrl.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "User{" +
                "mUid='" + mUid + '\'' +
                ", mEmail='" + mEmail + '\'' +
                ", mName='" + mName + '\'' +
                ", mPhotoUrl=" + mPhotoUrl +
                '}';
    }

    public static class Builder implements com.supercilex.robotscouter.data.util.Builder<User> {
        private final String mUid;
        private String mEmail;
        private String mName;
        private Uri mPhotoUrl;

        public Builder(String uid) {
            mUid = uid;
        }

        public Builder setEmail(String email) {
            mEmail = email;
            return this;
        }

        public Builder setName(String name) {
            mName = name;
            return this;
        }

        public Builder setPhotoUrl(Uri photoUrl) {
            mPhotoUrl = photoUrl;
            return this;
        }

        @Override
        public User build() {
            return new User(mUid, mEmail, mName, mPhotoUrl);
        }
    }
}

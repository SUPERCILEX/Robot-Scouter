package com.supercilex.robotscouter.data.model;

import android.net.Uri;

import com.supercilex.robotscouter.util.Constants;

public class User {
    private final String mUid;
    private String mEmail;
    private String mName;
    private Uri mPhotoUrl;

    private User(String uid, String email, String name, Uri photoUrl) {
        mUid = uid;
        mEmail = email;
        mName = name;
        mPhotoUrl = photoUrl;
    }

    public String getEmail() {
        return mEmail;
    }

    public void setEmail(String email) {
        mEmail = email;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getPhotoUrl() {
        return mPhotoUrl == null ? null : mPhotoUrl.toString();
    }

    public void setPhotoUrl(Uri photoUrl) {
        mPhotoUrl = photoUrl;
    }

    public void add() {
        Constants.FIREBASE_USERS.child(mUid).setValue(this);
    }

    public static class Builder {
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

        public User build() {
            return new User(mUid, mEmail, mName, mPhotoUrl);
        }
    }
}

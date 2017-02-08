package com.supercilex.robotscouter.data.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.util.Preconditions;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.PropertyName;
import com.google.firebase.database.ServerValue;
import com.supercilex.robotscouter.data.util.TeamHelper;
import com.supercilex.robotscouter.util.Constants;

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
    public Team() { // Needed for Firebase
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
    public TeamHelper getHelper() {
        return new TeamHelper(this);
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
        return mHasCustomName ? true : null; // NOPMD TODO https://github.com/pmd/pmd/issues/232
    }

    @Keep
    public void setHasCustomName(boolean hasCustomName) {
        mHasCustomName = hasCustomName;
    }

    @Keep
    @Nullable
    public Boolean getHasCustomMedia() {
        return mHasCustomMedia ? true : null; // NOPMD TODO https://github.com/pmd/pmd/issues/232
    }

    @Keep
    public void setHasCustomMedia(boolean hasCustomMedia) {
        mHasCustomMedia = hasCustomMedia;
    }

    @Keep
    @Nullable
    public Boolean getHasCustomWebsite() {
        return mHasCustomWebsite ? true : null; // NOPMD TODO https://github.com/pmd/pmd/issues/232
    }

    @Keep
    public void setHasCustomWebsite(boolean hasCustomWebsite) {
        mHasCustomWebsite = hasCustomWebsite;
    }

    @Keep
    @SuppressWarnings("SameReturnValue")
    @PropertyName(Constants.FIREBASE_TIMESTAMP)
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
            if (mTimestamp > team.getTimestamp()) {
                return 1;
            } else if (mTimestamp == team.getTimestamp()) {
                return 0;
            } else {
                return -1;
            }
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

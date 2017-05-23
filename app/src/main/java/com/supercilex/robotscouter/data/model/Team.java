package com.supercilex.robotscouter.data.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.text.TextUtils;

import com.firebase.ui.auth.util.Preconditions;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.PropertyName;
import com.supercilex.robotscouter.data.util.TeamHelper;

import static com.supercilex.robotscouter.util.ConstantsKt.FIREBASE_TIMESTAMP;

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
                            getBooleanForInt(source.readInt()),
                            source.readInt(),
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
    @Exclude private boolean mShouldUploadMediaToTba;
    @Exclude private int mMediaYear;
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
                 boolean shouldUploadMediaToTba,
                 int mediaYear,
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
        mShouldUploadMediaToTba = shouldUploadMediaToTba;
        mMediaYear = mediaYear;
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
        return mHasCustomName ? true : null;
    }

    @Keep
    public void setHasCustomName(boolean hasCustomName) {
        mHasCustomName = hasCustomName;
    }

    @Keep
    @Nullable
    public Boolean getHasCustomMedia() {
        return mHasCustomMedia ? true : null;
    }

    @Keep
    public void setHasCustomMedia(boolean hasCustomMedia) {
        mHasCustomMedia = hasCustomMedia;
    }

    @Keep
    @Nullable
    public Boolean getHasCustomWebsite() {
        return mHasCustomWebsite ? true : null;
    }

    @Keep
    public void setHasCustomWebsite(boolean hasCustomWebsite) {
        mHasCustomWebsite = hasCustomWebsite;
    }

    @Keep
    @Nullable
    public Boolean getShouldUploadMediaToTba() {
        return mShouldUploadMediaToTba ? true : null;
    }

    @Keep
    public void setShouldUploadMediaToTba(boolean shouldUploadMediaToTba) {
        mShouldUploadMediaToTba = shouldUploadMediaToTba;
    }

    @Keep
    public int getMediaYear() {
        return mMediaYear;
    }

    @Keep
    public void setMediaYear(int mediaYear) {
        mMediaYear = mediaYear;
    }

    @Keep
    @PropertyName(FIREBASE_TIMESTAMP)
    public Object getCurrentTimestamp() {
        return System.currentTimeMillis();
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
        dest.writeInt(getIntForBoolean(mShouldUploadMediaToTba));
        dest.writeInt(mMediaYear);
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

        return mNumber.equals(team.mNumber)
                && TextUtils.equals(mKey, team.mKey)
                && TextUtils.equals(mTemplateKey, team.mTemplateKey)
                && TextUtils.equals(mName, team.mName)
                && TextUtils.equals(mMedia, team.mMedia)
                && TextUtils.equals(mWebsite, team.mWebsite)
                && mHasCustomName == team.mHasCustomName
                && mHasCustomMedia == team.mHasCustomMedia
                && mHasCustomWebsite == team.mHasCustomWebsite
                && mShouldUploadMediaToTba == team.mShouldUploadMediaToTba
                && mMediaYear == team.mMediaYear
                && mTimestamp == team.mTimestamp;
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
        result = 31 * result + (mShouldUploadMediaToTba ? 1 : 0);
        result = 31 * result + mMediaYear;
        result = 31 * result + (int) (mTimestamp ^ (mTimestamp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return TextUtils.isEmpty(getName()) ? getNumber() : getNumber() + " - " + getName();
    }

    @Override
    public int compareTo(Team team) {
        int comparison = Long.valueOf(getNumberAsLong()).compareTo(team.getNumberAsLong());
        return comparison == 0 ?
                Long.valueOf(mTimestamp).compareTo(team.getTimestamp()) : comparison;
    }

    public static class Builder {
        private final String mNumber;
        private String mKey;
        private String mTemplateKey;
        private String mName;
        private String mMedia;
        private String mWebsite;
        private boolean mHasCustomName;
        private boolean mHasCustomMedia;
        private boolean mHasCustomWebsite;
        private boolean mShouldUploadMediaToTba;
        private int mMediaYear;
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
            if (team.getShouldUploadMediaToTba() != null) {
                mShouldUploadMediaToTba = team.getShouldUploadMediaToTba();
            }
            mMediaYear = team.getMediaYear();
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

        public Builder setShouldUploadMediaToTba(boolean shouldUploadMediaToTba) {
            mShouldUploadMediaToTba = shouldUploadMediaToTba;
            return this;
        }

        public Builder setMediaYear(int mediaYear) {
            mMediaYear = mediaYear;
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
                            mHasCustomMedia,
                            mHasCustomWebsite,
                            mShouldUploadMediaToTba,
                            mMediaYear,
                            mTimestamp);
        }
    }
}

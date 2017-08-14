package com.supercilex.robotscouter.data.model

import android.os.Parcel
import android.os.Parcelable
import android.support.annotation.Keep
import android.support.annotation.RestrictTo
import android.text.TextUtils
import com.google.firebase.database.Exclude
import com.google.firebase.database.PropertyName
import com.supercilex.robotscouter.util.FIREBASE_TIMESTAMP
import com.supercilex.robotscouter.util.data.defaultTemplateKey
import com.supercilex.robotscouter.util.data.readBooleanCompat
import com.supercilex.robotscouter.util.data.writeBooleanCompat

data class Team(@Exclude @get:Keep @set:Keep @set:RestrictTo(RestrictTo.Scope.TESTS) var number: String,
                @Exclude @get:Exclude @set:Exclude var key: String,
                @Exclude @get:Keep @set:Keep var templateKey: String = defaultTemplateKey,
                @Exclude @get:Keep @set:Keep var name: String? = null,
                @Exclude @get:Keep @set:Keep var media: String? = null,
                @Exclude @get:Keep @set:Keep var website: String? = null,
                @Exclude @get:Keep @set:Keep var hasCustomName: Boolean = false,
                @Exclude @get:Keep @set:Keep var hasCustomMedia: Boolean = false,
                @Exclude @get:Keep @set:Keep var hasCustomWebsite: Boolean = false,
                @Exclude @get:Keep @set:Keep var shouldUploadMediaToTba: Boolean = false,
                @Exclude @get:Keep @set:Keep var mediaYear: Int = 0,
                @Exclude @get:Exclude @set:Keep @set:RestrictTo(RestrictTo.Scope.TESTS) var timestamp: Long = 0) :
        Parcelable, Comparable<Team> {
    @get:Exclude
    val numberAsLong get() = number.toLong()

    // Empty no-arg constructor for Firebase
    constructor() : this("0", "")

    @Keep
    @PropertyName(FIREBASE_TIMESTAMP)
    fun getCurrentTimestamp(): Any = System.currentTimeMillis()

    override fun toString() = if (TextUtils.isEmpty(name)) number else number + " - " + name

    override operator fun compareTo(other: Team): Int {
        val comparison = numberAsLong.compareTo(other.numberAsLong)
        return if (comparison == 0) timestamp.compareTo(other.timestamp)
        else comparison
    }

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.apply {
            writeString(number)
            writeString(key)
            writeString(templateKey)
            writeString(name)
            writeString(media)
            writeString(website)
            writeBooleanCompat(hasCustomName)
            writeBooleanCompat(hasCustomMedia)
            writeBooleanCompat(hasCustomWebsite)
            writeBooleanCompat(shouldUploadMediaToTba)
            writeInt(mediaYear)
            writeLong(timestamp)
        }
    }

    companion object {
        @Suppress("unused")
        @Exclude
        @JvmField
        val CREATOR: Parcelable.Creator<Team> = object : Parcelable.Creator<Team> {
            override fun createFromParcel(source: Parcel): Team = source.run {
                Team(readString(),
                     readString(),
                     readString(),
                     readString(),
                     readString(),
                     readString(),
                     readBooleanCompat(),
                     readBooleanCompat(),
                     readBooleanCompat(),
                     readBooleanCompat(),
                     readInt(),
                     readLong())
            }

            override fun newArray(size: Int): Array<Team?> = arrayOfNulls(size)
        }
    }
}

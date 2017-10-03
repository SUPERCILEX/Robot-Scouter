package com.supercilex.robotscouter.data.model

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.support.annotation.Keep
import android.support.annotation.RestrictTo
import android.text.TextUtils
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import com.supercilex.robotscouter.util.FIRESTORE_TIMESTAMP
import com.supercilex.robotscouter.util.data.defaultTemplateId
import com.supercilex.robotscouter.util.data.readBooleanCompat
import com.supercilex.robotscouter.util.data.readBundleAsMap
import com.supercilex.robotscouter.util.data.writeBooleanCompat
import com.supercilex.robotscouter.util.uid
import java.util.Date

data class Team(@Exclude @get:Keep @set:Keep @set:RestrictTo(RestrictTo.Scope.TESTS) var number: Long,
                @Exclude @get:Exclude var id: String,
                @Exclude @get:Keep @set:Keep @set:RestrictTo(RestrictTo.Scope.TESTS)
                var owners: Map<String, Long> = mapOf(uid!! to number),
                @Exclude @get:Keep @set:Keep @set:RestrictTo(RestrictTo.Scope.TESTS)
                var activeTokens: MutableMap<String, Date> = emptyMap<String, Date>().toMutableMap(),
                @Exclude @get:Keep @set:Keep @set:RestrictTo(RestrictTo.Scope.TESTS)
                var pendingApprovals: Map<String, String> = emptyMap(),
                @Exclude @get:Keep @set:Keep var templateId: String = defaultTemplateId,
                @Exclude @get:Keep @set:Keep var name: String? = null,
                @Exclude @get:Keep @set:Keep var media: String? = null,
                @Exclude @get:Keep @set:Keep var website: String? = null,
                @Exclude @get:Keep @set:Keep var hasCustomName: Boolean = false,
                @Exclude @get:Keep @set:Keep var hasCustomMedia: Boolean = false,
                @Exclude @get:Keep @set:Keep var hasCustomWebsite: Boolean = false,
                @Exclude @get:Keep @set:Keep var shouldUploadMediaToTba: Boolean = false,
                @Exclude @get:Keep @set:Keep var mediaYear: Int = 0,
                @Exclude @get:Exclude @set:Keep @set:RestrictTo(RestrictTo.Scope.TESTS)
                var timestamp: Date = Date(0)) :
        Parcelable, Comparable<Team> {
    // Empty no-arg constructor for Firebase
    constructor() : this(0, "")

    @Keep
    @PropertyName(FIRESTORE_TIMESTAMP)
    fun getCurrentTimestamp() = Date()

    override fun toString() = if (TextUtils.isEmpty(name)) number.toString() else "$number - $name"

    override operator fun compareTo(other: Team): Int {
        val comparison = number.compareTo(other.number)
        return if (comparison == 0) timestamp.compareTo(other.timestamp)
        else comparison
    }

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.apply {
            writeLong(number)
            writeString(id)
            writeBundle(Bundle().apply { owners.forEach { putLong(it.key, it.value) } })
            writeBundle(Bundle().apply { activeTokens.forEach { putLong(it.key, it.value.time) } })
            writeBundle(Bundle().apply { pendingApprovals.forEach { putString(it.key, it.value) } })
            writeString(templateId)
            writeString(name)
            writeString(media)
            writeString(website)
            writeBooleanCompat(hasCustomName)
            writeBooleanCompat(hasCustomMedia)
            writeBooleanCompat(hasCustomWebsite)
            writeBooleanCompat(shouldUploadMediaToTba)
            writeInt(mediaYear)
            writeLong(timestamp.time)
        }
    }

    companion object {
        @Suppress("unused")
        @Exclude
        @JvmField
        val CREATOR: Parcelable.Creator<Team> = object : Parcelable.Creator<Team> {
            override fun createFromParcel(source: Parcel): Team = source.run {
                Team(readLong(),
                     readString(),
                     readBundleAsMap(),
                     readBundleAsMap { Date(getLong(it)) }.toMutableMap(),
                     readBundleAsMap(),
                     readString(),
                     readString(),
                     readString(),
                     readString(),
                     readBooleanCompat(),
                     readBooleanCompat(),
                     readBooleanCompat(),
                     readBooleanCompat(),
                     readInt(),
                     Date(readLong()))
            }

            override fun newArray(size: Int): Array<Team?> = arrayOfNulls(size)
        }
    }
}

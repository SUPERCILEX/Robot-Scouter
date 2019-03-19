package com.supercilex.robotscouter.core.model

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.annotation.RestrictTo
import com.google.firebase.firestore.Exclude
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
import java.util.Date

@Parcelize
data class Team(
        @Exclude
        @get:Keep
        @set:Keep
        @set:RestrictTo(RestrictTo.Scope.TESTS)
        var number: Long,

        @Exclude
        @get:Exclude
        var id: String,

        @Exclude
        @get:Keep
        @set:Keep
        @set:RestrictTo(RestrictTo.Scope.TESTS)
        var owners: Map<String, Long> = emptyMap(),

        @Exclude
        @get:Keep
        @set:Keep
        var templateId: String = TemplateType.DEFAULT.id.toString(),

        @SerializedName("nickname")
        @Exclude
        @get:Keep
        @set:Keep
        var name: String? = null,

        @Exclude
        @get:Keep
        @set:Keep
        var media: String? = null,

        @Exclude
        @get:Keep
        @set:Keep
        var website: String? = null,

        @Exclude
        @get:Keep
        @set:Keep
        var hasCustomName: Boolean = false,

        @Exclude
        @get:Keep
        @set:Keep
        var hasCustomMedia: Boolean = false,

        @Exclude
        @get:Keep
        @set:Keep
        var hasCustomWebsite: Boolean = false,

        @Exclude
        @get:Keep
        @set:Keep
        var shouldUploadMediaToTba: Boolean = false,

        @Exclude
        @get:Keep
        @set:Keep
        var mediaYear: Int = 0,

        @Exclude
        @get:Keep
        @set:Keep
        var timestamp: Date = Date(0)
) : Parcelable, Comparable<Team> {
    // Empty no-arg constructor for Firebase
    constructor() : this(0, "")

    override fun toString() = if (name.isNullOrBlank()) {
        number.toString()
    } else {
        "$number - $name"
    }

    override operator fun compareTo(other: Team): Int {
        val comparison = number.compareTo(other.number)
        return if (comparison == 0) timestamp.compareTo(other.timestamp)
        else comparison
    }
}

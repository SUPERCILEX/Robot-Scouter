package com.supercilex.robotscouter.data.model

import android.support.annotation.Keep
import com.google.firebase.firestore.Exclude
import java.util.Date

data class Scout(
        @Exclude
        @get:Exclude
        val id: String,

        @Exclude
        @get:Keep
        val templateId: String,

        @Exclude
        @get:Keep
        val name: String? = null,

        @Exclude
        @get:Keep
        val timestamp: Date = Date(),

        @Exclude
        @get:Exclude
        val metrics: List<Metric<*>> = emptyList()
)

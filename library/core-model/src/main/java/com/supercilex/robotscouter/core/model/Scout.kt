package com.supercilex.robotscouter.core.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

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
        val timestamp: Timestamp = Timestamp.now(),

        @Exclude
        @get:Exclude
        val metrics: List<Metric<*>> = emptyList()
)

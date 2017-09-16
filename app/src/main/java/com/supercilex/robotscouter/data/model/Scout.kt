package com.supercilex.robotscouter.data.model

import android.support.annotation.Keep
import com.google.firebase.firestore.Exclude

data class Scout(@get:Exclude val id: String,
                 @get:Keep val templateId: String,
                 @get:Keep val name: String? = null,
                 @get:Keep val timestamp: Long = System.currentTimeMillis(),
                 @get:Exclude val metrics: List<Metric<*>> = emptyList())

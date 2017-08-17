package com.supercilex.robotscouter.data.model

import android.support.annotation.Keep
import com.google.firebase.database.Exclude

data class Scout(@Exclude @get:Keep val name: String?,
                 @Exclude @get:Keep val templateKey: String,
                 @Exclude @get:Keep val metrics: List<Metric<*>>)

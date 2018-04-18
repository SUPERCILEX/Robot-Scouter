package com.supercilex.robotscouter.core.model

import android.support.annotation.Keep
import com.google.firebase.firestore.Exclude

data class Device(
        @Exclude
        @get:Exclude
        @Keep
        var id: String,

        @Exclude
        @Keep
        @get:Keep
        @set:Keep
        var name: String? = null
)

package com.supercilex.robotscouter.core.model

import androidx.annotation.Keep
import com.google.firebase.firestore.Exclude

data class User(
        @Keep
        @Exclude
        @get:Exclude
        val uid: String,

        @Keep
        @Exclude
        @get:Keep
        val email: String? = null,

        @Keep
        @Exclude
        @get:Keep
        val phoneNumber: String? = null,

        @Keep
        @Exclude
        @get:Keep
        val name: String? = null,

        @Keep
        @Exclude
        @get:Keep
        val photoUrl: String? = null
)

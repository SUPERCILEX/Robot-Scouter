package com.supercilex.robotscouter.core.model

import android.net.Uri
import androidx.annotation.Keep
import com.google.firebase.firestore.Exclude

data class User(
        @Exclude
        @get:Exclude
        val uid: String,

        @Exclude
        @get:Keep
        val email: String? = null,

        @Exclude
        @get:Keep
        val phoneNumber: String? = null,

        @Exclude
        @get:Keep
        val name: String? = null,

        @Exclude
        @get:Exclude
        private val _photoUrl: Uri? = null
) {
    @Exclude
    @get:Keep
    val photoUrl: String? = _photoUrl?.toString()
}

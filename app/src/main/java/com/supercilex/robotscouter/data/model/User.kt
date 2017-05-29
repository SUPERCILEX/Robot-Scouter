package com.supercilex.robotscouter.data.model

import android.net.Uri
import android.support.annotation.Keep
import com.google.firebase.database.Exclude
import com.supercilex.robotscouter.data.util.UserHelper

data class User(@Exclude @get:Exclude val uid: String,
                @Exclude @get:Keep val email: String? = null,
                @Exclude @get:Keep val name: String? = null,
                @Exclude @get:Exclude private val _photoUrl: Uri? = null) {
    @Exclude @get:Keep val photoUrl: String? = _photoUrl?.toString()
}

val User.helper: UserHelper get() = UserHelper(this)

package com.supercilex.robotscouter.core.data

import android.os.Build
import android.os.Handler
import android.os.Looper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.supercilex.robotscouter.common.FIRESTORE_DEFAULT_TEMPLATES
import com.supercilex.robotscouter.common.FIRESTORE_DELETION_QUEUE
import com.supercilex.robotscouter.common.FIRESTORE_TEAMS
import com.supercilex.robotscouter.common.FIRESTORE_TEMPLATES
import com.supercilex.robotscouter.common.FIRESTORE_USERS
import com.supercilex.robotscouter.core.fullVersionName

val mainHandler = Handler(Looper.getMainLooper())
val Thread.isMain get() = this === mainHandler.looper.thread

val user get() = FirebaseAuth.getInstance().currentUser
val uid get() = user?.uid
val isSignedIn get() = user != null
val isFullUser get() = isSignedIn && !checkNotNull(user).isAnonymous

val usersRef = FirebaseFirestore.getInstance().collection(FIRESTORE_USERS)
val teamsRef = FirebaseFirestore.getInstance().collection(FIRESTORE_TEAMS)
val templatesRef = FirebaseFirestore.getInstance().collection(FIRESTORE_TEMPLATES)
val defaultTemplatesRef = FirebaseFirestore.getInstance().collection(FIRESTORE_DEFAULT_TEMPLATES)
val deletionQueueRef = FirebaseFirestore.getInstance().collection(FIRESTORE_DELETION_QUEUE)

val debugInfo: String
    get() =
        """
        |- Robot Scouter version: $fullVersionName
        |- Android OS version: ${Build.VERSION.SDK_INT}
        |- User id: $uid
        """.trimMargin()

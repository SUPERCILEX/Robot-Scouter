package com.supercilex.robotscouter.core.data

import com.bumptech.glide.Glide
import com.google.firebase.appindexing.FirebaseAppIndex
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.await
import com.supercilex.robotscouter.core.logFailures
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async

fun cleanup(): Deferred<*> = async {
    Glide.get(RobotScouter).clearDiskCache()
    cleanupJobs()
    FirebaseAppIndex.getInstance().removeAll().await()
}.logFailures()

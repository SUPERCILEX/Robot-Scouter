package com.supercilex.robotscouter.core.data

import com.bumptech.glide.Glide
import com.google.firebase.appindexing.FirebaseAppIndex
import com.google.firebase.functions.FirebaseFunctions
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.await
import com.supercilex.robotscouter.core.logFailures
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

fun CoroutineScope.cleanup(): Deferred<*> = async(Dispatchers.IO) {
    Glide.get(RobotScouter).clearDiskCache()
    cleanupJobs()
    FirebaseAppIndex.getInstance().removeAll().await()
}.logFailures()

fun emptyTrash(ids: List<String>? = null) = FirebaseFunctions.getInstance()
        .getHttpsCallable("emptyTrash")
        .call(ids)
        .logFailures("Ids: $ids")

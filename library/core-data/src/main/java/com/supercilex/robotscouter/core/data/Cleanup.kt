package com.supercilex.robotscouter.core.data

import com.bumptech.glide.Glide
import com.google.firebase.appindexing.FirebaseAppIndex
import com.google.firebase.functions.FirebaseFunctions
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.await
import com.supercilex.robotscouter.core.logFailures
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.IO
import kotlinx.coroutines.experimental.async

fun cleanup(): Deferred<*> = GlobalScope.async(Dispatchers.IO) {
    Glide.get(RobotScouter).clearDiskCache()
    cleanupJobs()
    FirebaseAppIndex.getInstance().removeAll().await()
}.logFailures()

fun emptyTrash(ids: List<String>? = null) = FirebaseFunctions.getInstance()
        .getHttpsCallable("emptyTrash")
        .call(ids)
        .logFailures("Ids: $ids")

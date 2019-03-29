package com.supercilex.robotscouter.core.data

import com.bumptech.glide.Glide
import com.google.firebase.appindexing.FirebaseAppIndex
import com.google.firebase.functions.FirebaseFunctions
import com.supercilex.robotscouter.core.RobotScouter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.invoke
import kotlinx.coroutines.launch

fun cleanup() {
    GlobalScope.launch {
        Dispatchers.IO { Glide.get(RobotScouter).clearDiskCache() }
        cleanupJobs()
    }
    FirebaseAppIndex.getInstance().removeAll().logFailures("cleanup")
}

fun emptyTrash(ids: List<String>? = null) = FirebaseFunctions.getInstance()
        .getHttpsCallable("emptyTrash")
        .call(ids)
        .logFailures("emptyTrash", ids)

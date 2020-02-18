package com.supercilex.robotscouter.core.data

import com.bumptech.glide.Glide
import com.google.firebase.appindexing.FirebaseAppIndex
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
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

fun emptyTrash(ids: List<String>? = null) = Firebase.functions
        .getHttpsCallable("clientApi")
        .call(mapOf("operation" to "empty-trash", "ids" to ids))
        .logFailures("emptyTrash", ids)

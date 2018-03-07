package com.supercilex.robotscouter.ui.scouting

import com.google.firebase.firestore.CollectionReference
import com.supercilex.robotscouter.data.model.Metric
import com.supercilex.robotscouter.util.FIRESTORE_POSITION
import com.supercilex.robotscouter.util.data.LifecycleAwareFirestoreArray
import com.supercilex.robotscouter.util.data.ViewModelBase
import com.supercilex.robotscouter.util.data.metricParser

class MetricListHolder : ViewModelBase<CollectionReference>() {
    lateinit var metrics: LifecycleAwareFirestoreArray<Metric<*>>
        private set

    override fun onCreate(args: CollectionReference) {
        metrics = LifecycleAwareFirestoreArray({ args.orderBy(FIRESTORE_POSITION) }, metricParser)
        metrics.keepAlive = true
    }

    override fun onCleared() {
        super.onCleared()
        metrics.keepAlive = false
    }
}

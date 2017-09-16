package com.supercilex.robotscouter.ui.scouting

import com.firebase.ui.firestore.FirestoreArray
import com.firebase.ui.firestore.ObservableSnapshotArray
import com.google.firebase.firestore.CollectionReference
import com.supercilex.robotscouter.data.model.Metric
import com.supercilex.robotscouter.util.FIRESTORE_POSITION
import com.supercilex.robotscouter.util.data.KeepAliveListener
import com.supercilex.robotscouter.util.data.METRIC_PARSER
import com.supercilex.robotscouter.util.data.ViewModelBase

class MetricListHolder : ViewModelBase<CollectionReference>() {
    lateinit var metrics: ObservableSnapshotArray<Metric<*>>

    override fun onCreate(args: CollectionReference) {
        metrics = FirestoreArray(args.orderBy(FIRESTORE_POSITION), METRIC_PARSER)
        metrics.addChangeEventListener(KeepAliveListener)
    }

    override fun onCleared() {
        super.onCleared()
        metrics.removeChangeEventListener(KeepAliveListener)
    }
}

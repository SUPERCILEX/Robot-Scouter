package com.supercilex.robotscouter.ui.scouting

import android.arch.lifecycle.DefaultLifecycleObserver
import android.arch.lifecycle.LifecycleOwner
import com.firebase.ui.firestore.FirestoreArray
import com.firebase.ui.firestore.ObservableSnapshotArray
import com.google.firebase.firestore.CollectionReference
import com.supercilex.robotscouter.data.model.Metric
import com.supercilex.robotscouter.util.FIRESTORE_POSITION
import com.supercilex.robotscouter.util.data.KeepAliveListener
import com.supercilex.robotscouter.util.data.ListenerRegistrationLifecycleOwner
import com.supercilex.robotscouter.util.data.ViewModelBase
import com.supercilex.robotscouter.util.data.metricParser
import com.supercilex.robotscouter.util.log

class MetricListHolder : ViewModelBase<CollectionReference>(), DefaultLifecycleObserver {
    lateinit var metrics: ObservableSnapshotArray<Metric<*>>
        private set

    override fun onCreate(args: CollectionReference) {
        metrics = FirestoreArray(args.orderBy(FIRESTORE_POSITION).log(), metricParser)
        ListenerRegistrationLifecycleOwner.lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        metrics.addChangeEventListener(KeepAliveListener)
    }

    override fun onStop(owner: LifecycleOwner) {
        metrics.removeChangeEventListener(KeepAliveListener)
    }

    override fun onCleared() {
        super.onCleared()
        ListenerRegistrationLifecycleOwner.lifecycle.removeObserver(this)
        onStop(ListenerRegistrationLifecycleOwner)
    }
}

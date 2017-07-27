package com.supercilex.robotscouter.ui.scouting

import android.app.Application
import com.firebase.ui.database.FirebaseArray
import com.firebase.ui.database.ObservableSnapshotArray
import com.google.firebase.database.DatabaseReference
import com.supercilex.robotscouter.data.model.Metric
import com.supercilex.robotscouter.ui.ViewModelBase
import com.supercilex.robotscouter.util.data.ChangeEventListenerBase
import com.supercilex.robotscouter.util.data.model.METRIC_PARSER

class MetricListHolder(app: Application) : ViewModelBase<DatabaseReference>(app) {
    lateinit var metrics: ObservableSnapshotArray<Metric<*>>

    private val listener = object : ChangeEventListenerBase() {}

    override fun onCreate(args: DatabaseReference) {
        metrics = FirebaseArray(args, METRIC_PARSER)
        metrics.addChangeEventListener(listener)
    }

    override fun onCleared() {
        super.onCleared()
        metrics.removeChangeEventListener(listener)
    }
}

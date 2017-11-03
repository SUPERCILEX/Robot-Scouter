package com.supercilex.robotscouter.util.data.model

import android.arch.lifecycle.DefaultLifecycleObserver
import android.arch.lifecycle.LifecycleOwner
import com.firebase.ui.firestore.FirestoreArray
import com.firebase.ui.firestore.ObservableSnapshotArray
import com.google.firebase.firestore.Query
import com.supercilex.robotscouter.data.model.Scout
import com.supercilex.robotscouter.util.LateinitVal
import com.supercilex.robotscouter.util.data.KeepAliveListener
import com.supercilex.robotscouter.util.data.ListenerRegistrationLifecycleOwner
import com.supercilex.robotscouter.util.data.ViewModelBase
import com.supercilex.robotscouter.util.data.scoutParser

class ScoutsHolder : ViewModelBase<Query>(), DefaultLifecycleObserver {
    var scouts: ObservableSnapshotArray<Scout> by LateinitVal()

    override fun onCreate(args: Query) {
        scouts = FirestoreArray(args, scoutParser)
        ListenerRegistrationLifecycleOwner.lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        scouts.addChangeEventListener(KeepAliveListener)
    }

    override fun onStop(owner: LifecycleOwner) {
        scouts.removeChangeEventListener(KeepAliveListener)
    }

    override fun onCleared() {
        super.onCleared()
        ListenerRegistrationLifecycleOwner.lifecycle.removeObserver(this)
        onStop(ListenerRegistrationLifecycleOwner)
    }
}

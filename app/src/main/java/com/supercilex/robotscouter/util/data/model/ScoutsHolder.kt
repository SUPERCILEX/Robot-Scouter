package com.supercilex.robotscouter.util.data.model

import com.firebase.ui.firestore.FirestoreArray
import com.firebase.ui.firestore.ObservableSnapshotArray
import com.google.firebase.firestore.Query
import com.supercilex.robotscouter.data.model.Scout
import com.supercilex.robotscouter.util.LateinitVal
import com.supercilex.robotscouter.util.data.KeepAliveListener
import com.supercilex.robotscouter.util.data.ViewModelBase
import com.supercilex.robotscouter.util.data.scoutParser

class ScoutsHolder : ViewModelBase<Query>() {
    var scouts: ObservableSnapshotArray<Scout> by LateinitVal()

    override fun onCreate(args: Query) {
        scouts = FirestoreArray(args, scoutParser)
        scouts.addChangeEventListener(KeepAliveListener)
    }

    override fun onCleared() {
        super.onCleared()
        scouts.removeChangeEventListener(KeepAliveListener)
    }
}

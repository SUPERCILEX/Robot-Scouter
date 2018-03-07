package com.supercilex.robotscouter.util.data.model

import com.supercilex.robotscouter.data.model.Scout
import com.supercilex.robotscouter.util.data.LifecycleAwareFirestoreArray
import com.supercilex.robotscouter.util.data.QueryGenerator
import com.supercilex.robotscouter.util.data.ViewModelBase
import com.supercilex.robotscouter.util.data.scoutParser

class ScoutsHolder : ViewModelBase<QueryGenerator>() {
    lateinit var scouts: LifecycleAwareFirestoreArray<Scout>
        private set

    override fun onCreate(args: QueryGenerator) {
        scouts = LifecycleAwareFirestoreArray(args, scoutParser)
        scouts.keepAlive = true
    }

    public override fun onCleared() {
        super.onCleared()
        scouts.keepAlive = false
    }
}

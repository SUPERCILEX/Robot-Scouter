package com.supercilex.robotscouter.core.data.model

import androidx.lifecycle.SavedStateHandle
import com.supercilex.robotscouter.core.data.LifecycleAwareFirestoreArray
import com.supercilex.robotscouter.core.data.QueryGenerator
import com.supercilex.robotscouter.core.data.ViewModelBase
import com.supercilex.robotscouter.core.model.Scout

class ScoutsHolder(state: SavedStateHandle) : ViewModelBase<QueryGenerator>(state) {
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

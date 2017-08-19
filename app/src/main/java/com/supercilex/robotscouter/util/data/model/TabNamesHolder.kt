package com.supercilex.robotscouter.util.data.model

import com.firebase.ui.database.ObservableSnapshotArray
import com.supercilex.robotscouter.util.data.ChangeEventListenerBase
import com.supercilex.robotscouter.util.data.TemplateNamesLiveData
import com.supercilex.robotscouter.util.data.ViewModelBase

class TabNamesHolder : ViewModelBase<Nothing?>() {
    lateinit var namesListener: ObservableSnapshotArray<String>
        private set

    private val keepAliveListener = object : ChangeEventListenerBase {}

    override fun onCreate(args: Nothing?) {
        namesListener = TemplateNamesLiveData.value!!
        namesListener.addChangeEventListener(keepAliveListener)
    }

    override fun onCleared() {
        super.onCleared()
        namesListener.removeChangeEventListener(keepAliveListener)
    }
}

package com.supercilex.robotscouter.util.data.model

import android.app.Application
import com.firebase.ui.database.FirebaseIndexArray
import com.firebase.ui.database.ObservableSnapshotArray
import com.firebase.ui.database.SnapshotParser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.Query
import com.supercilex.robotscouter.util.FIREBASE_NAME
import com.supercilex.robotscouter.util.data.ChangeEventListenerBase
import com.supercilex.robotscouter.util.data.ViewModelBase

class TabNamesHolder(app: Application) : ViewModelBase<Pair<Query, DatabaseReference>>(app) {
    lateinit var namesListener: ObservableSnapshotArray<String?>
        private set

    private val keepAliveListener = object : ChangeEventListenerBase {}

    override fun onCreate(args: Pair<Query, DatabaseReference>) {
        namesListener = FirebaseIndexArray(
                args.first,
                args.second,
                SnapshotParser { it.child(FIREBASE_NAME).getValue(String::class.java) })
        namesListener.addChangeEventListener(keepAliveListener)
    }

    override fun onCleared() {
        super.onCleared()
        namesListener.removeChangeEventListener(keepAliveListener)
    }
}

package com.supercilex.robotscouter.ui.scout

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import com.google.firebase.crash.FirebaseCrash
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.supercilex.robotscouter.data.util.getScoutIndicesRef
import com.supercilex.robotscouter.ui.ViewModelBase

class ScoutKeysHolder(app: Application) : ViewModelBase<String>(app), ValueEventListener {
    val keysListener = MutableLiveData<List<String>>()

    lateinit var ref: DatabaseReference

    override fun onCreate(args: String) {
        ref = getScoutIndicesRef(args)
        ref.addValueEventListener(this)
    }

    override fun onDataChange(snapshot: DataSnapshot) {
        keysListener.value = snapshot.children.map { it.key }.reversed()
    }

    override fun onCleared() = ref.removeEventListener(this)

    override fun onCancelled(error: DatabaseError) = FirebaseCrash.report(error.toException())
}

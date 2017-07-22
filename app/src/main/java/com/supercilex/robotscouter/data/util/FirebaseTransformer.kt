package com.supercilex.robotscouter.data.util

import android.support.annotation.CallSuper

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.crash.FirebaseCrash
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener

abstract class FirebaseTransformer(private val fromQuery: Query,
                                   protected val toRef: DatabaseReference) : ValueEventListener {
    private val completeTask = TaskCompletionSource<DataSnapshot>()

    protected abstract fun transform(transformSnapshot: DataSnapshot): Task<Nothing?>

    @CallSuper
    override fun onDataChange(snapshot: DataSnapshot) {
        transform(snapshot).addOnCompleteListener { task ->
            if (task.isSuccessful) completeTask.setResult(snapshot)
            else completeTask.setException(task.exception!!)
        }
    }

    fun performTransformation(): Task<DataSnapshot> {
        fromQuery.addListenerForSingleValueEvent(this)
        return completeTask.task
    }

    override fun onCancelled(error: DatabaseError) {
        FirebaseCrash.report(error.toException())
        completeTask.setException(error.toException())
    }
}

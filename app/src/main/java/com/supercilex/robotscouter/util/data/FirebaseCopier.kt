package com.supercilex.robotscouter.util.data

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.Query

import java.util.HashMap

open class FirebaseCopier(from: Query, to: DatabaseReference) : FirebaseTransformer(from, to) {
    public override fun transform(transformSnapshot: DataSnapshot): Task<Nothing?> =
            if (transformSnapshot.value == null) Tasks.forResult(null)
            else copyTo(transformSnapshot, toRef)

    companion object {
        fun copyTo(copySnapshot: DataSnapshot, to: DatabaseReference): Task<Nothing?> =
                HashMap<String, Any?>().let {
                    deepCopy(it, copySnapshot)
                    to.updateChildren(it).continueWith { null }
                }

        private fun deepCopy(values: MutableMap<String, Any?>, from: DataSnapshot) {
            val children = from.children
            if (children.iterator().hasNext()) {
                for (snapshot in children) {
                    val data = HashMap<String, Any?>()
                    data.put(".priority", snapshot.priority)
                    values.put(snapshot.key, data)

                    deepCopy(data, snapshot)
                }
            } else {
                values.put(".value", from.value)
            }
        }
    }
}

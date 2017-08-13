package com.supercilex.robotscouter.util.data

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.Query

open class FirebaseCopier(from: Query, to: DatabaseReference) : FirebaseTransformer(from, to) {
    public override fun transform(transformSnapshot: DataSnapshot): Task<Nothing?> =
            if (transformSnapshot.value == null) Tasks.forResult(null)
            else copySnapshots(transformSnapshot, toRef)
}

package com.supercilex.robotscouter.util.data.model

import com.google.firebase.firestore.SetOptions
import com.supercilex.robotscouter.data.model.User
import com.supercilex.robotscouter.util.FIRESTORE_PREFS
import com.supercilex.robotscouter.util.data.QueryGenerator
import com.supercilex.robotscouter.util.deletionQueueRef
import com.supercilex.robotscouter.util.logFailures
import com.supercilex.robotscouter.util.uid
import com.supercilex.robotscouter.util.usersRef

val userRef get() = getUserRef(uid!!)

val userPrefsQueryGenerator: QueryGenerator = { getUserPrefs(it.uid) }
val userPrefs get() = getUserPrefs(uid!!)

val userDeletionQueue get() = deletionQueueRef.document(uid!!)

fun User.add() {
    val ref = getUserRef(uid)
    ref.set(this, SetOptions.merge()).logFailures(ref, this)
}

private fun getUserRef(uid: String) = usersRef.document(uid)

private fun getUserPrefs(uid: String) = getUserRef(uid).collection(FIRESTORE_PREFS)

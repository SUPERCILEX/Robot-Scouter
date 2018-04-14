package com.supercilex.robotscouter.core.data.model

import com.google.firebase.firestore.SetOptions
import com.supercilex.robotscouter.common.FIRESTORE_PREFS
import com.supercilex.robotscouter.core.data.QueryGenerator
import com.supercilex.robotscouter.core.data.deletionQueueRef
import com.supercilex.robotscouter.core.data.uid
import com.supercilex.robotscouter.core.data.usersRef
import com.supercilex.robotscouter.core.logFailures
import com.supercilex.robotscouter.core.model.User

val userRef get() = getUserRef(uid!!)

val userPrefsQueryGenerator: QueryGenerator = { getUserPrefs(it.uid) }
val userPrefs get() = getUserPrefs(uid!!)

val userDeletionQueue get() = deletionQueueRef.document(uid!!)

internal fun User.add() {
    val ref = getUserRef(uid)
    ref.set(this, SetOptions.merge()).logFailures(ref, this)
}

private fun getUserRef(uid: String) = usersRef.document(uid)

private fun getUserPrefs(uid: String) = getUserRef(uid).collection(FIRESTORE_PREFS)

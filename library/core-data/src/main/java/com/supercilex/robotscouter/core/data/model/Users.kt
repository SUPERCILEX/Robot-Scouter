package com.supercilex.robotscouter.core.data.model

import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.supercilex.robotscouter.common.FIRESTORE_PREFS
import com.supercilex.robotscouter.common.FIRESTORE_PREV_UID
import com.supercilex.robotscouter.common.FIRESTORE_TOKEN
import com.supercilex.robotscouter.core.data.QueryGenerator
import com.supercilex.robotscouter.core.data.deletionQueueRef
import com.supercilex.robotscouter.core.data.logFailures
import com.supercilex.robotscouter.core.data.uid
import com.supercilex.robotscouter.core.data.usersRef

val userRef get() = getUserRef(checkNotNull(uid))

internal val userPrefsQueryGenerator: QueryGenerator = { getUserPrefs(it.uid) }
val userPrefs get() = getUserPrefs(checkNotNull(uid))

val userDeletionQueue get() = deletionQueueRef.document(checkNotNull(uid))

fun transferUserData(prevUid: String, token: String) = Firebase.functions
        .getHttpsCallable("clientApi")
        .call(mapOf(
                "operation" to "transfer-user-data",
                FIRESTORE_PREV_UID to prevUid,
                FIRESTORE_TOKEN to token
        ))
        .logFailures("transferUserData", prevUid, token)

private fun getUserRef(uid: String) = usersRef.document(uid)

private fun getUserPrefs(uid: String) = getUserRef(uid).collection(FIRESTORE_PREFS)

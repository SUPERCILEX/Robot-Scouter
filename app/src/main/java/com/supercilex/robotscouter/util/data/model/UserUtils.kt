package com.supercilex.robotscouter.util.data.model

import com.supercilex.robotscouter.data.model.User
import com.supercilex.robotscouter.util.FIRESTORE_PREFS
import com.supercilex.robotscouter.util.FIRESTORE_USERS
import com.supercilex.robotscouter.util.uid

val userPrefs get() = getUserPrefs(uid!!)

fun User.add() {
    val userRef = getUserRef(uid)

    TODO()
}

private fun getUserRef(uid: String) = FIRESTORE_USERS.document(uid)

private fun getUserPrefs(uid: String) = getUserRef(uid).collection(FIRESTORE_PREFS)

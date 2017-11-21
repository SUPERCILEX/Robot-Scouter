package com.supercilex.robotscouter.util.data.model

import com.google.firebase.firestore.SetOptions
import com.supercilex.robotscouter.data.model.User
import com.supercilex.robotscouter.util.FIRESTORE_PREFS
import com.supercilex.robotscouter.util.uid
import com.supercilex.robotscouter.util.users

val userRef get() = getUserRef(uid!!)

val userPrefs get() = getUserPrefs(uid!!)

fun User.add() {
    getUserRef(uid).set(this, SetOptions.merge())
}

private fun getUserRef(uid: String) = users.document(uid)

private fun getUserPrefs(uid: String) = getUserRef(uid).collection(FIRESTORE_PREFS)

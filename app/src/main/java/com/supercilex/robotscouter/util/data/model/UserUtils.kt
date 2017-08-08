package com.supercilex.robotscouter.util.data.model

import android.text.TextUtils
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.Exclude
import com.supercilex.robotscouter.data.model.User
import com.supercilex.robotscouter.util.FIREBASE_PREFS
import com.supercilex.robotscouter.util.FIREBASE_TEAM_INDICES
import com.supercilex.robotscouter.util.FIREBASE_USERS
import com.supercilex.robotscouter.util.data.FirebaseCopier
import com.supercilex.robotscouter.util.uid

val userPrefs: DatabaseReference get() = getUserPrefs(uid!!)

fun User.add() {
    val userRef = getUserRef(uid)

    User::class.java.declaredMethods.filter {
        (it.name.startsWith("get") || it.name.startsWith("is"))
                && it.declaredAnnotations.find { it.annotationClass.java == Exclude::class.java } == null
    }.forEach {
        userRef.child(it.name.substringAfter("get").substringAfter("is").decapitalize())
                .setValue(it.invoke(this))
    }
}

fun transferUserData(prevUid: String?) {
    if (TextUtils.isEmpty(prevUid)) return
    prevUid!!

    val prevPrefsRef = getUserPrefs(prevUid)
    object : FirebaseCopier(prevPrefsRef, userPrefs) {
        override fun onDataChange(snapshot: DataSnapshot) {
            super.onDataChange(snapshot)
            prevPrefsRef.removeValue()
        }
    }.performTransformation()

    val prevTeamRef = FIREBASE_TEAM_INDICES.child(prevUid)
    object : FirebaseCopier(prevTeamRef, teamIndicesRef) {
        override fun onDataChange(snapshot: DataSnapshot) {
            super.onDataChange(snapshot)
            prevTeamRef.removeValue()
        }
    }.performTransformation()

    val prevScoutTemplatesRef = getTemplateIndicesRef(prevUid)
    object : FirebaseCopier(prevScoutTemplatesRef, templateIndicesRef) {
        override fun onDataChange(snapshot: DataSnapshot) {
            super.onDataChange(snapshot)
            prevScoutTemplatesRef.removeValue()
        }
    }.performTransformation()
}

private fun getUserRef(uid: String): DatabaseReference = FIREBASE_USERS.child(uid)

private fun getUserPrefs(uid: String): DatabaseReference = getUserRef(uid).child(FIREBASE_PREFS)

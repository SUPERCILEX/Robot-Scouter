package com.supercilex.robotscouter.util

import android.text.TextUtils
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.supercilex.robotscouter.data.model.User
import com.supercilex.robotscouter.data.util.FirebaseCopier

val templateIndicesRef: DatabaseReference get() = getTemplateIndicesRef(uid!!)

private fun getTemplateIndicesRef(uid: String): DatabaseReference =
        FIREBASE_USERS.child(uid).child(SCOUT_TEMPLATE_INDICES)

fun User.add() = FIREBASE_USERS.child(uid).setValue(this).continueWith { null }

fun transferUserData(prevUid: String?) {
    if (TextUtils.isEmpty(prevUid)) return
    prevUid!!

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

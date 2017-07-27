package com.supercilex.robotscouter.util.data.model

import android.text.TextUtils
import com.google.firebase.database.DataSnapshot
import com.supercilex.robotscouter.data.model.User
import com.supercilex.robotscouter.util.FIREBASE_TEAM_INDICES
import com.supercilex.robotscouter.util.FIREBASE_USERS
import com.supercilex.robotscouter.util.data.FirebaseCopier

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

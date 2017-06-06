package com.supercilex.robotscouter.data.util

import android.text.TextUtils
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.supercilex.robotscouter.data.model.User
import com.supercilex.robotscouter.util.FIREBASE_TEAM_INDICES
import com.supercilex.robotscouter.util.FIREBASE_USERS
import com.supercilex.robotscouter.util.SCOUT_TEMPLATE_INDICES
import com.supercilex.robotscouter.util.uid

val templateIndicesRef: DatabaseReference get() = getTemplateIndicesRef(uid!!)

private fun getTemplateIndicesRef(uid: String): DatabaseReference {
    return FIREBASE_USERS.child(uid).child(SCOUT_TEMPLATE_INDICES)
}

data class UserHelper(private val user: User) {
    fun add() {
        FIREBASE_USERS.child(user.uid).setValue(user)
    }

    fun transferData(prevUid: String?) {
        if (TextUtils.isEmpty(prevUid)) return
        prevUid!!

        val prevTeamRef = FIREBASE_TEAM_INDICES.child(prevUid)
        object : FirebaseCopier(prevTeamRef, TeamHelper.getIndicesRef()) {
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

    override fun toString() = user.toString()
}

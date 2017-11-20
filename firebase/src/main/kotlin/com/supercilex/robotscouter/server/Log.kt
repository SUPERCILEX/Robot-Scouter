package com.supercilex.robotscouter.server

import com.supercilex.robotscouter.server.utils.FIRESTORE_NAME
import com.supercilex.robotscouter.server.utils.FIRESTORE_NUMBER
import com.supercilex.robotscouter.server.utils.getTeamsQuery
import com.supercilex.robotscouter.server.utils.getTemplatesQuery
import com.supercilex.robotscouter.server.utils.getTrashedTeamsQuery
import com.supercilex.robotscouter.server.utils.getTrashedTemplatesQuery
import com.supercilex.robotscouter.server.utils.types.QuerySnapshot
import kotlin.js.Promise

fun logUserData(uid: String): Promise<*> {
    console.log("Logging user data for id: $uid")
    return Promise.all(arrayOf(
            getTeamsQuery(uid).get().then {
                console.log("Team ids:")
                it.logTeamIds()
            },
            getTrashedTeamsQuery(uid).get().then {
                console.log("Trashed team ids:")
                it.logTeamIds()
            },
            getTemplatesQuery(uid).get().then {
                console.log("Template ids:")
                it.logNameIds()
            },
            getTrashedTemplatesQuery(uid).get().then {
                console.log("Trashed team ids:")
                it.logNameIds()
            }
    ))
}

private fun QuerySnapshot.logTeamIds() {
    console.log(docs.map {
        "${it.data()[FIRESTORE_NAME] ?: it.data()[FIRESTORE_NUMBER]}: ${it.id}"
    })
}

private fun QuerySnapshot.logNameIds() {
    console.log(docs.map {
        "${it.data()[FIRESTORE_NAME]}: ${it.id}"
    })
}

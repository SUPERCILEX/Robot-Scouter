package com.supercilex.robotscouter.server

import com.supercilex.robotscouter.server.utils.getTeamsQuery
import com.supercilex.robotscouter.server.utils.getTemplatesQuery
import com.supercilex.robotscouter.server.utils.getTrashedTeamsQuery
import com.supercilex.robotscouter.server.utils.getTrashedTemplatesQuery
import com.supercilex.robotscouter.server.utils.types.QuerySnapshot
import kotlin.js.Promise

fun logUserData(uid: String): Promise<*> = Promise.all(arrayOf(
        getTeamsQuery(uid).get().then {
            console.log("Team ids:")
            it.logIds()
        },
        getTrashedTeamsQuery(uid).get().then {
            console.log("Trashed team ids:")
            it.logIds()
        },
        getTemplatesQuery(uid).get().then {
            console.log("Template ids:")
            it.logIds()
        },
        getTrashedTemplatesQuery(uid).get().then {
            console.log("Trashed team ids:")
            it.logIds()
        }
))

private fun QuerySnapshot.logIds() {
    console.log(docs.map { it.id })
}

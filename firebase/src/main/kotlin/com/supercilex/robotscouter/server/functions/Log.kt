package com.supercilex.robotscouter.server.functions

import com.supercilex.robotscouter.server.utils.getTeamsQuery
import com.supercilex.robotscouter.server.utils.getTemplatesQuery
import com.supercilex.robotscouter.server.utils.getTrashedTeamsQuery
import com.supercilex.robotscouter.server.utils.getTrashedTemplatesQuery
import com.supercilex.robotscouter.server.utils.toTeamString
import com.supercilex.robotscouter.server.utils.toTemplateString
import com.supercilex.robotscouter.server.utils.types.DocumentSnapshot
import com.supercilex.robotscouter.server.utils.types.Message
import com.supercilex.robotscouter.server.utils.types.QuerySnapshot
import kotlin.js.Promise

fun logUserData(message: Message): Promise<*>? {
    val uid = message.json["uid"] as String

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
                console.log("Trashed template ids:")
                it.logNameIds()
            }
    ))
}

private fun QuerySnapshot.logTeamIds() = logIds { toTeamString() }

private fun QuerySnapshot.logNameIds() = logIds { toTemplateString() }

private fun QuerySnapshot.logIds(process: DocumentSnapshot.() -> String) {
    console.log("${docs.size} items: " + docs.map(process).toString())
}

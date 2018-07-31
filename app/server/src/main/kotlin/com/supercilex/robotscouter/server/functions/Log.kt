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
import kotlinx.coroutines.experimental.asPromise
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.await
import kotlinx.coroutines.experimental.awaitAll
import kotlin.js.Promise

fun logUserData(message: Message): Promise<*>? {
    val uid = message.json["uid"] as String

    console.log("Logging user data for id: $uid")
    return async {
        val teams = async {
            val snapshot = getTeamsQuery(uid).get().await()
            console.log("${snapshot.size} teams:\n" + snapshot.prettyPrintTeams())
        }
        val trashedTeams = async {
            val snapshot = getTrashedTeamsQuery(uid).get().await()
            console.log("${snapshot.size} trashed teams:\n" + snapshot.prettyPrintTeams())
        }
        val templates = async {
            val snapshot = getTemplatesQuery(uid).get().await()
            console.log("${snapshot.size} templates:\n" + snapshot.prettyPrintTemplates())
        }
        val trashedTemplates = async {
            val snapshot = getTrashedTemplatesQuery(uid).get().await()
            console.log("${snapshot.size} trashed templates:\n" + snapshot.prettyPrintTemplates())
        }

        awaitAll(teams, trashedTeams, templates, trashedTemplates)
    }.asPromise()
}

private fun QuerySnapshot.prettyPrintTeams() = prettyPrint { toTeamString() }

private fun QuerySnapshot.prettyPrintTemplates() = prettyPrint { toTemplateString() }

private fun QuerySnapshot.prettyPrint(process: DocumentSnapshot.() -> String) =
        docs.joinToString(separator = ",\n", transform = process)

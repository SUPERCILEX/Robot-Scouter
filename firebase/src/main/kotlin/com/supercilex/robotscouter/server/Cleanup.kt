package com.supercilex.robotscouter.server

import com.supercilex.robotscouter.server.utils.getTeamsQuery
import com.supercilex.robotscouter.server.utils.getTemplatesQuery
import com.supercilex.robotscouter.server.utils.types.DocumentSnapshot
import com.supercilex.robotscouter.server.utils.users
import kotlin.js.Promise

private const val FIRESTORE_LAST_LOGIN = "lastLogin"
private const val INACTIVE_DAYS = 365

fun cleanup(): Promise<*> {
    console.log("Looking for users that haven't opened Robot Scouter for over a year.")
    return users.where(
            FIRESTORE_LAST_LOGIN,
            "<",
            modules.moment().subtract(INACTIVE_DAYS, "days").toDate()
    ).get().then { users ->
        Promise.all(users.docs.map {
            deleteAllData(it)
        }.toTypedArray())
    }.then { it }
}

private fun deleteAllData(user: DocumentSnapshot): Promise<Array<out Array<out String>>> {
    console.log("Deleting all data for user:\n${JSON.stringify(user.data())}")
    val id: String = user.id
    return Promise.all(arrayOf(
            deleteTeams(id),
            deleteTemplates(id)
    )).then { it }
}

private fun deleteTeams(userId: String): Promise<Array<out String>> = getTeamsQuery(userId)
        .get().then { teams ->
    Promise.all(teams.docs.map {
        deleteTeam(it)
    }.toTypedArray())
}.then { it }

private fun deleteTeam(team: DocumentSnapshot): Promise<String> {
    console.log("Deleting team: ${JSON.stringify(team.data())}")
    return Promise.resolve("null")
}

private fun deleteTemplates(userId: String): Promise<Array<out String>> = getTemplatesQuery(userId)
        .get().then { templates ->
    Promise.all(templates.docs.map {
        deleteTemplate(it)
    }.toTypedArray())
}.then { it }

private fun deleteTemplate(template: DocumentSnapshot): Promise<String> {
    console.log("Deleting template: ${JSON.stringify(template.data())}")
    return Promise.resolve("null")
}

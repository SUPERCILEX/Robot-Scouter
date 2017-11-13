package com.supercilex.robotscouter.server

import com.supercilex.robotscouter.server.utils.teams
import com.supercilex.robotscouter.server.utils.templates
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
    }
}

private fun deleteAllData(user: DocumentSnapshot): Promise<Array<out Promise<Array<out String>>>> {
    console.log("Deleting all data for user:\n${JSON.stringify(user.data())}")
    val id: String = user.id
    return Promise.all(arrayOf(
            deleteTeams(id),
            deleteTemplates(id)
    ))
}

private fun deleteTeams(userId: String): Promise<Promise<Array<out String>>> = teams.where(
        "owners.$userId", ">=", 0
).get().then { teams ->
    Promise.all(teams.docs.map {
        deleteTeam(it)
    }.toTypedArray())
}

private fun deleteTeam(team: DocumentSnapshot): Promise<String> {
    console.log("Deleting team: ${JSON.stringify(team.data())}")
    return Promise.resolve("null")
}

private fun deleteTemplates(userId: String): Promise<Promise<Array<out String>>> = templates.where(
        "owners.$userId", ">=", modules.moment(0).toDate()
).get().then { templates ->
    Promise.all(templates.docs.map {
        deleteTemplate(it)
    }.toTypedArray())
}

private fun deleteTemplate(template: DocumentSnapshot): Promise<String> {
    console.log("Deleting template: ${JSON.stringify(template.data())}")
    return Promise.resolve("null")
}

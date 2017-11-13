package com.supercilex.robotscouter.server

import kotlin.js.Promise

private const val FIRESTORE_LAST_LOGIN = "lastLogin"
private const val INACTIVE_DAYS = 365

fun cleanup(): Promise<dynamic> {
    console.log("Looking for users that haven't opened Robot Scouter for over a year.")
    return (users.where(
            FIRESTORE_LAST_LOGIN,
            "<",
            modules.moment().subtract(INACTIVE_DAYS, "days").toDate()
    ).get() as Promise<dynamic>).then<dynamic> { users ->
        Promise.all((users.docs as Array<dynamic>).map {
            deleteAllData(it)
        }.toTypedArray())
    }
}

private fun deleteAllData(user: dynamic): Promise<dynamic> {
    console.log("Deleting all data for user:\n${JSON.stringify(user.data())}")
    val id: String = user.id
    return Promise.all(arrayOf(
            deleteTeams(id),
            deleteTemplates(id)
    ))
}

private fun deleteTeams(userId: String): Promise<dynamic> = teams.where(
        "owners.$userId", ">=", 0
).get().then<dynamic> { teams ->
    Promise.all((teams.docs as Array<dynamic>).map {
        deleteTeam(it)
    }.toTypedArray())
}

private fun deleteTeam(team: dynamic): Promise<dynamic> {
    console.log("Deleting team: ${JSON.stringify(team.data())}")
    return Promise.Companion.resolve("null")
}

private fun deleteTemplates(userId: String): Promise<dynamic> = templates.where(
        "owners.$userId", ">=", modules.moment(0).toDate()
).get().then<dynamic> { templates ->
    Promise.all((templates.docs as Array<dynamic>).map {
        deleteTemplate(it)
    }.toTypedArray())
}

private fun deleteTemplate(template: dynamic): Promise<dynamic> {
    console.log("Deleting template: ${JSON.stringify(template.data())}")
    return Promise.Companion.resolve("null")
}

package com.supercilex.robotscouter.server

import com.supercilex.robotscouter.server.utils.getTeamsQuery
import com.supercilex.robotscouter.server.utils.getTemplatesQuery
import com.supercilex.robotscouter.server.utils.getTrashedTeamsQuery
import com.supercilex.robotscouter.server.utils.getTrashedTemplatesQuery
import com.supercilex.robotscouter.server.utils.toTeamString
import com.supercilex.robotscouter.server.utils.toTemplateString
import com.supercilex.robotscouter.server.utils.types.DocumentSnapshot
import com.supercilex.robotscouter.server.utils.types.Query
import com.supercilex.robotscouter.server.utils.users
import kotlin.js.Promise

private const val FIRESTORE_LAST_LOGIN = "lastLogin"
private const val MAX_INACTIVE_USER_DAYS = 30 // TODO

fun wipeUserData(): Promise<*> {
    console.log("Looking for users that haven't opened Robot Scouter for over a year.")
    return users.where(
            FIRESTORE_LAST_LOGIN,
            "<",
            modules.moment().subtract(MAX_INACTIVE_USER_DAYS, "days").toDate()
    ).get().then { users ->
        Promise.all(users.docs.map {
            deleteAllData(it)
        }.toTypedArray())
    }.then { it }
}

fun emptyTrash(): Promise<*> {
    console.log("Emptying trash for all users.")
    return users.get().then { users ->
        Promise.all(users.docs.map {
            val userId = it.id
            Promise.all(arrayOf(
                    deleteTeams(getTrashedTeamsQuery(userId)),
                    deleteTemplates(getTrashedTemplatesQuery(userId))
            ))
        }.toTypedArray())
    }
}

private fun deleteAllData(user: DocumentSnapshot): Promise<Unit> {
    console.log("Deleting all data for user:\n${JSON.stringify(user.data())}")
    val id: String = user.id
    return Promise.all(arrayOf(
            deleteTeams(getTeamsQuery(id)),
            deleteTemplates(getTemplatesQuery(id))
    )).then {
        deleteUser(user)
    }.then { Unit }
}

private fun deleteUser(user: DocumentSnapshot): Promise<Unit> {
    console.log("Deleting user: ${JSON.stringify(user.data())}")
//    user.userPrefs.delete()
//    user.ref.delete()
    return Promise.resolve(Unit)
}

private fun deleteTeams(query: Query): Promise<Unit> = query.get().then { teams ->
    Promise.all(teams.docs.map {
        deleteTeam(it)
    }.toTypedArray())
}.then { Unit }

private fun deleteTeam(team: DocumentSnapshot): Promise<Unit> {
    console.log("Deleting team: ${team.toTeamString()}")
    return Promise.resolve(Unit)
}

private fun deleteTemplates(query: Query): Promise<Unit> = query
        .get().then { templates ->
    Promise.all(templates.docs.map {
        deleteTemplate(it)
    }.toTypedArray())
}.then { Unit }

private fun deleteTemplate(template: DocumentSnapshot): Promise<Unit> {
    console.log("Deleting template: ${template.toTemplateString()}")
    return Promise.resolve(Unit)
}

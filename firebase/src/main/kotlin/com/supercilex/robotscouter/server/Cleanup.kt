package com.supercilex.robotscouter.server

import com.supercilex.robotscouter.server.utils.FIRESTORE_EMAIL
import com.supercilex.robotscouter.server.utils.FIRESTORE_METRICS
import com.supercilex.robotscouter.server.utils.FIRESTORE_PHONE_NUMBER
import com.supercilex.robotscouter.server.utils.FIRESTORE_SCOUTS
import com.supercilex.robotscouter.server.utils.FIRESTORE_TIMESTAMP
import com.supercilex.robotscouter.server.utils.delete
import com.supercilex.robotscouter.server.utils.getTeamsQuery
import com.supercilex.robotscouter.server.utils.getTemplatesQuery
import com.supercilex.robotscouter.server.utils.getTrashedTeamsQuery
import com.supercilex.robotscouter.server.utils.getTrashedTemplatesQuery
import com.supercilex.robotscouter.server.utils.toTeamString
import com.supercilex.robotscouter.server.utils.toTemplateString
import com.supercilex.robotscouter.server.utils.types.DocumentSnapshot
import com.supercilex.robotscouter.server.utils.types.Query
import com.supercilex.robotscouter.server.utils.userPrefs
import com.supercilex.robotscouter.server.utils.users
import kotlin.js.Promise

private const val FIRESTORE_LAST_LOGIN = "lastLogin"
private const val MAX_INACTIVE_USER_DAYS = 365
private const val MAX_INACTIVE_ANONYMOUS_USER_DAYS = 60
private const val TRASH_TIMEOUT = 30

fun deleteUnusedData(): Promise<*> {
    console.log("Looking for users that haven't opened Robot Scouter for over a year" +
                        " or anonymous users that haven't opened Robot Scouter in over 60 days.")
    return Promise.all(arrayOf(
            deleteUnusedData(users.where(
                    FIRESTORE_LAST_LOGIN,
                    "<",
                    modules.moment().subtract(MAX_INACTIVE_USER_DAYS, "days").toDate()
            )),
            deleteUnusedData(users.where( // TODO remove once enough people have upgraded
                    FIRESTORE_LAST_LOGIN,
                    "<",
                    modules.moment().subtract(30, "days").toDate()
            ).where(
                    FIRESTORE_EMAIL, "==", null
            )),
            deleteUnusedData(users.where(
                    FIRESTORE_LAST_LOGIN,
                    "<",
                    modules.moment().subtract(MAX_INACTIVE_ANONYMOUS_USER_DAYS, "days").toDate()
            ).where(
                    FIRESTORE_EMAIL, "==", null
            ).where(
                    FIRESTORE_PHONE_NUMBER, "==", null
            ))
    ))
}

private fun deleteUnusedData(userQuery: Query): Promise<Unit> = userQuery.get().then { users ->
    Promise.all(users.docs.map {
        deleteAllData(it)
    }.toTypedArray())
}.then { Unit }

fun emptyTrash(): Promise<*> {
    console.log("Emptying trash for all users.")
    return Promise.Companion.resolve(Unit) // TODO
    return users.get().then { users ->
        Promise.all(users.docs.map {
            val userId = it.id
            Promise.all(arrayOf(
                    deleteTeams(getTrashedTeamsQuery(userId).where(
                            FIRESTORE_TIMESTAMP,
                            "<",
                            modules.moment().subtract(TRASH_TIMEOUT, "days").toDate()
                    )),
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
    console.log("Deleting user: ${user.id}")
    return user.userPrefs.delete().then {
        user.ref.delete()
    }.then { Unit }
}

private fun deleteTeams(query: Query): Promise<Unit> = query.get().then { teams ->
    Promise.all(teams.docs.map {
        deleteTeam(it)
    }.toTypedArray())
}.then { Unit }

private fun deleteTeam(team: DocumentSnapshot): Promise<Unit> {
    console.log("Deleting team: ${team.toTeamString()}")
    return team.ref.collection(FIRESTORE_SCOUTS).delete {
        it.ref.collection(FIRESTORE_METRICS).delete()
    }.then {
        team.ref.delete()
    }.then { Unit }
}

private fun deleteTemplates(query: Query): Promise<Unit> = query
        .get().then { templates ->
    Promise.all(templates.docs.map {
        deleteTemplate(it)
    }.toTypedArray())
}.then { Unit }

private fun deleteTemplate(template: DocumentSnapshot): Promise<Unit> {
    console.log("Deleting template: ${template.toTemplateString()}")
    return template.ref.collection(FIRESTORE_METRICS).delete().then {
        template.ref.delete()
    }.then { Unit }
}

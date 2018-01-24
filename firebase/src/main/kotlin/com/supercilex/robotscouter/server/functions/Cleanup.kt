package com.supercilex.robotscouter.server.functions

import com.supercilex.robotscouter.server.modules
import com.supercilex.robotscouter.server.utils.FIRESTORE_EMAIL
import com.supercilex.robotscouter.server.utils.FIRESTORE_LAST_LOGIN
import com.supercilex.robotscouter.server.utils.FIRESTORE_METRICS
import com.supercilex.robotscouter.server.utils.FIRESTORE_OWNERS
import com.supercilex.robotscouter.server.utils.FIRESTORE_PHONE_NUMBER
import com.supercilex.robotscouter.server.utils.FIRESTORE_SCOUTS
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
import kotlin.js.Json
import kotlin.js.Promise

private const val MAX_INACTIVE_USER_DAYS = 365
private const val MAX_INACTIVE_ANONYMOUS_USER_DAYS = 45

fun deleteUnusedData(): Promise<*> {
    console.log("Looking for users that haven't opened Robot Scouter for over a year" +
                        " or anonymous users that haven't opened Robot Scouter in over 60 days.")
    return Promise.all(arrayOf(
            deleteUnusedData(users.where(
                    FIRESTORE_LAST_LOGIN,
                    "<",
                    modules.moment().subtract(MAX_INACTIVE_USER_DAYS, "days").toDate()
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

private fun deleteUnusedData(userQuery: Query): Promise<Unit> = userQuery.process {
    console.log("Deleting all data for user:\n${JSON.stringify(data())}")

    val userId = id
    Promise.all(arrayOf(
            getTeamsQuery(userId).process {
                deleteIfSingleOwner(userId) { deleteTeam(it) }
            },
            getTemplatesQuery(userId).process {
                deleteIfSingleOwner(userId) { deleteTemplate(it) }
            }
    )).then {
        deleteUser(this)
    }
}

fun emptyTrash(): Promise<*> {
    console.log("Emptying trash for all users.")
    return users.process {
        val userId = id
        Promise.all(arrayOf(
                getTrashedTeamsQuery(userId).process {
                    deleteIfSingleOwner(userId) { deleteTeam(this) }
                },
                getTrashedTemplatesQuery(userId).process {
                    deleteIfSingleOwner(userId) { deleteTemplate(this) }
                }
        ))
    }
}

private fun deleteUser(user: DocumentSnapshot): Promise<Unit> {
    console.log("Deleting user: ${user.id}")
    return user.userPrefs.delete().then {
        user.ref.delete()
    }.then { Unit }
}

private fun deleteTeam(team: DocumentSnapshot): Promise<Unit> {
    console.log("Deleting team: ${team.toTeamString()}")
    return team.ref.collection(FIRESTORE_SCOUTS).delete {
        it.ref.collection(FIRESTORE_METRICS).delete()
    }.then {
        team.ref.delete()
    }.then { Unit }
}

private fun deleteTemplate(template: DocumentSnapshot): Promise<Unit> {
    console.log("Deleting template: ${template.toTemplateString()}")
    return template.ref.collection(FIRESTORE_METRICS).delete().then {
        template.ref.delete()
    }.then { Unit }
}

private fun Query.process(block: DocumentSnapshot.() -> Promise<*>): Promise<Unit> = get().then {
    Promise.all(it.docs.map(block).toTypedArray())
}.then { Unit }

fun DocumentSnapshot.deleteIfSingleOwner(
        userId: String,
        delete: (DocumentSnapshot) -> Promise<*>
): Promise<*> {
    @Suppress("UNCHECKED_CAST_TO_NATIVE_INTERFACE") // We know its type
    val owners = get(FIRESTORE_OWNERS) as Json
    //language=JavaScript
    return if (js("Object.keys(owners).length") as Int > 1) {
        //language=undefined
        console.log("Removing $userId's ownership of $id")
        //language=JavaScript
        js("delete owners[userId]")
        ref.update(FIRESTORE_OWNERS, owners)
    } else {
        delete(this)
    }
}

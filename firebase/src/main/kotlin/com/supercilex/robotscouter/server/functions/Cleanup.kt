package com.supercilex.robotscouter.server.functions

import com.supercilex.robotscouter.server.modules
import com.supercilex.robotscouter.server.utils.FIRESTORE_ACTIVE_TOKENS
import com.supercilex.robotscouter.server.utils.FIRESTORE_CONTENT_ID
import com.supercilex.robotscouter.server.utils.FIRESTORE_EMAIL
import com.supercilex.robotscouter.server.utils.FIRESTORE_LAST_LOGIN
import com.supercilex.robotscouter.server.utils.FIRESTORE_METRICS
import com.supercilex.robotscouter.server.utils.FIRESTORE_OWNERS
import com.supercilex.robotscouter.server.utils.FIRESTORE_PENDING_APPROVALS
import com.supercilex.robotscouter.server.utils.FIRESTORE_PHONE_NUMBER
import com.supercilex.robotscouter.server.utils.FIRESTORE_SCOUTS
import com.supercilex.robotscouter.server.utils.FIRESTORE_SCOUT_TYPE
import com.supercilex.robotscouter.server.utils.FIRESTORE_SHARE_TOKEN_TYPE
import com.supercilex.robotscouter.server.utils.FIRESTORE_SHARE_TYPE
import com.supercilex.robotscouter.server.utils.FIRESTORE_TEAM_TYPE
import com.supercilex.robotscouter.server.utils.FIRESTORE_TEMPLATE_TYPE
import com.supercilex.robotscouter.server.utils.FIRESTORE_TIMESTAMP
import com.supercilex.robotscouter.server.utils.FIRESTORE_TYPE
import com.supercilex.robotscouter.server.utils.FieldValue
import com.supercilex.robotscouter.server.utils.delete
import com.supercilex.robotscouter.server.utils.deletionQueue
import com.supercilex.robotscouter.server.utils.getTeamsQuery
import com.supercilex.robotscouter.server.utils.getTemplatesQuery
import com.supercilex.robotscouter.server.utils.getTrashedTeamsQuery
import com.supercilex.robotscouter.server.utils.getTrashedTemplatesQuery
import com.supercilex.robotscouter.server.utils.teams
import com.supercilex.robotscouter.server.utils.templates
import com.supercilex.robotscouter.server.utils.toMap
import com.supercilex.robotscouter.server.utils.toTeamString
import com.supercilex.robotscouter.server.utils.toTemplateString
import com.supercilex.robotscouter.server.utils.types.CollectionReference
import com.supercilex.robotscouter.server.utils.types.DocumentSnapshot
import com.supercilex.robotscouter.server.utils.types.Query
import com.supercilex.robotscouter.server.utils.userPrefs
import com.supercilex.robotscouter.server.utils.users
import kotlin.js.Date
import kotlin.js.Json
import kotlin.js.Promise

private const val MAX_INACTIVE_USER_DAYS = 365
private const val MAX_INACTIVE_ANONYMOUS_USER_DAYS = 45
private const val TRASH_TIMEOUT_DAYS = 30

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
                deleteIfSingleOwner(userId) { deleteTeam(this) }
            },
            getTemplatesQuery(userId).process {
                deleteIfSingleOwner(userId) { deleteTemplate(this) }
            }
    )).then {
        deleteUser(this)
    }
}

fun emptyTrash(): Promise<*> {
    console.log("Emptying trash for all users.")

    val new = deletionQueue.process {
        val userId = id
        Promise.all(data().toMap<Json>().map { (key, data) ->
            val deletionTime = data[FIRESTORE_TIMESTAMP] as Date
            if ((modules.moment().diff(deletionTime, "days") as Int) < TRASH_TIMEOUT_DAYS) {
                return@map Promise.resolve<Unit?>(null)
            }

            when (data[FIRESTORE_TYPE]) {
                FIRESTORE_TEAM_TYPE -> teams.doc(key).get().then {
                    it.deleteIfSingleOwner(userId) { deleteTeam(this) }
                }
                FIRESTORE_SCOUT_TYPE -> teams.doc(data[FIRESTORE_CONTENT_ID] as String)
                        .collection(FIRESTORE_SCOUTS)
                        .doc(key)
                        .run {
                            console.log("Deleting scout: $id")
                            Promise.all(arrayOf(delete(), collection(FIRESTORE_METRICS).delete()))
                        }
                FIRESTORE_TEMPLATE_TYPE -> templates.doc(key).get().then {
                    it.deleteIfSingleOwner(userId) { deleteTemplate(this) }
                }
                FIRESTORE_SHARE_TOKEN_TYPE -> {
                    fun CollectionReference.deleteShareToken(
                    ) = Promise.all((data[FIRESTORE_CONTENT_ID] as Array<String>).map {
                        doc(it).run {
                            Promise.all(arrayOf(
                                    update("$FIRESTORE_ACTIVE_TOKENS.$key", FieldValue.delete()),
                                    update(FIRESTORE_PENDING_APPROVALS, FieldValue.delete())
                            ))
                        }
                    }.toTypedArray()).then { Unit }

                    console.log("Deleting share token: $key")
                    when (data[FIRESTORE_SHARE_TYPE] as Int) {
                        FIRESTORE_TEAM_TYPE -> teams.deleteShareToken()
                        FIRESTORE_TEMPLATE_TYPE -> templates.deleteShareToken()
                        else -> error("Unknown share type: ${data[FIRESTORE_SHARE_TYPE]}")
                    }
                }
                else -> error("Unknown type: ${data[FIRESTORE_TYPE]}")
            }.then {
                ref.update(key, FieldValue.delete())
            }.then { Unit }
        }.toTypedArray()).then {
            if (it.none { it == null }) ref.delete()
        }
    }
    // TODO remove at some point
    val deprecated = users.process {
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

    return Promise.all(arrayOf(new, deprecated))
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
        delete: DocumentSnapshot.() -> Promise<*>
): Promise<*> {
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE") // We know its type
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

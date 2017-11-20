package com.supercilex.robotscouter.server.utils

import com.supercilex.robotscouter.server.modules
import com.supercilex.robotscouter.server.utils.types.CollectionReference
import com.supercilex.robotscouter.server.utils.types.Firestore
import com.supercilex.robotscouter.server.utils.types.Query

private const val FIRESTORE_USERS = "users"
private const val FIRESTORE_TEAMS = "teams"
private const val FIRESTORE_TEMPLATES = "templates"

val users: CollectionReference
    get() = modules.firestore.collection(FIRESTORE_USERS)
val teams: CollectionReference
    get() = modules.firestore.collection(FIRESTORE_TEAMS)
val templates: CollectionReference
    get() = modules.firestore.collection(FIRESTORE_TEMPLATES)

fun getTeamsQuery(uid: String): Query = teams.where("owners.$uid", ">=", 0)

fun getTrashedTeamsQuery(uid: String): Query = teams.where("owners.$uid", "<", 0)

fun getTemplatesQuery(uid: String): Query =
        templates.where("owners.$uid", ">=", modules.moment(0).toDate())

fun getTrashedTemplatesQuery(uid: String): Query =
        templates.where("owners.$uid", "<", modules.moment(0).toDate())

class Modules(
        val firestore: Firestore,
        val moment: dynamic
)

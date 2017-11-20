package com.supercilex.robotscouter.server.utils

import com.supercilex.robotscouter.server.modules
import com.supercilex.robotscouter.server.utils.types.CollectionReference
import com.supercilex.robotscouter.server.utils.types.DocumentSnapshot
import com.supercilex.robotscouter.server.utils.types.Firestore
import com.supercilex.robotscouter.server.utils.types.Query
import kotlin.js.Date

const val FIRESTORE_OWNERS = "owners"
const val FIRESTORE_NAME = "name"
const val FIRESTORE_NUMBER = "number"

private const val FIRESTORE_USERS = "users"
private const val FIRESTORE_PREFS = "prefs"
private const val FIRESTORE_TEAMS = "teams"
private const val FIRESTORE_TEMPLATES = "templates"

val users: CollectionReference
    get() = modules.firestore.collection(FIRESTORE_USERS)
val DocumentSnapshot.userPrefs: CollectionReference
    get() = ref.collection(FIRESTORE_PREFS)
val teams: CollectionReference
    get() = modules.firestore.collection(FIRESTORE_TEAMS)
val templates: CollectionReference
    get() = modules.firestore.collection(FIRESTORE_TEMPLATES)

private val epoch: Date = modules.moment(0).toDate()

fun getTeamsQuery(uid: String): Query = teams.where("$FIRESTORE_OWNERS.$uid", ">=", 0)

fun getTrashedTeamsQuery(uid: String): Query = teams.where("$FIRESTORE_OWNERS.$uid", "<", 0)

fun getTemplatesQuery(uid: String): Query =
        templates.where("$FIRESTORE_OWNERS.$uid", ">=", epoch)

fun getTrashedTemplatesQuery(uid: String): Query =
        templates.where("$FIRESTORE_OWNERS.$uid", "<", epoch)

class Modules(
        val firestore: Firestore,
        val moment: dynamic
)

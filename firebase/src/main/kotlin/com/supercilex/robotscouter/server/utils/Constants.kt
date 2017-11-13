package com.supercilex.robotscouter.server.utils

import com.supercilex.robotscouter.server.modules
import com.supercilex.robotscouter.server.utils.types.CollectionReference
import com.supercilex.robotscouter.server.utils.types.Firestore

private const val FIRESTORE_USERS = "users"
private const val FIRESTORE_TEAMS = "teams"
private const val FIRESTORE_TEMPLATES = "templates"

val users: CollectionReference
    get() = modules.firestore.collection(FIRESTORE_USERS)
val teams: CollectionReference
    get() = modules.firestore.collection(FIRESTORE_TEAMS)
val templates: CollectionReference
    get() = modules.firestore.collection(FIRESTORE_TEMPLATES)

class Modules(
        val firestore: Firestore,
        val moment: dynamic
)

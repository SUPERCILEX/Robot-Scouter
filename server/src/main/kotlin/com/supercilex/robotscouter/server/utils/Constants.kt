package com.supercilex.robotscouter.server.utils

import com.supercilex.robotscouter.common.FIRESTORE_DEFAULT_TEMPLATES
import com.supercilex.robotscouter.common.FIRESTORE_DELETION_QUEUE
import com.supercilex.robotscouter.common.FIRESTORE_OWNERS
import com.supercilex.robotscouter.common.FIRESTORE_PREFS
import com.supercilex.robotscouter.common.FIRESTORE_TEAMS
import com.supercilex.robotscouter.common.FIRESTORE_TEMPLATES
import com.supercilex.robotscouter.common.FIRESTORE_USERS
import com.supercilex.robotscouter.server.require
import com.supercilex.robotscouter.server.utils.types.CollectionReference
import com.supercilex.robotscouter.server.utils.types.DocumentSnapshot
import com.supercilex.robotscouter.server.utils.types.Query
import com.supercilex.robotscouter.server.utils.types.admin
import kotlin.js.Date

const val FIRESTORE_EMAIL = "email"
const val FIRESTORE_PHONE_NUMBER = "phoneNumber"

const val FIRESTORE_TEAM_TYPE = 0
const val FIRESTORE_SCOUT_TYPE = 1
const val FIRESTORE_TEMPLATE_TYPE = 2
const val FIRESTORE_SHARE_TOKEN_TYPE = 3

val firestore by lazy { admin.firestore() }
val auth by lazy { admin.auth() }
val moment: dynamic by lazy { require("moment") }

val defaultTemplates: CollectionReference
    get() = firestore.collection(FIRESTORE_DEFAULT_TEMPLATES)
val users: CollectionReference
    get() = firestore.collection(FIRESTORE_USERS)
val DocumentSnapshot.userPrefs: CollectionReference
    get() = ref.collection(FIRESTORE_PREFS)
val teams: CollectionReference
    get() = firestore.collection(FIRESTORE_TEAMS)
val templates: CollectionReference
    get() = firestore.collection(FIRESTORE_TEMPLATES)
val deletionQueue: CollectionReference
    get() = firestore.collection(FIRESTORE_DELETION_QUEUE)

val epoch: Date by lazy { Date(0) }

fun getTeamsQuery(uid: String): Query = teams.where("$FIRESTORE_OWNERS.$uid", ">=", 0)

fun getTrashedTeamsQuery(uid: String): Query = teams.where("$FIRESTORE_OWNERS.$uid", "<", 0)

fun getTemplatesQuery(uid: String): Query =
        templates.where("$FIRESTORE_OWNERS.$uid", ">=", epoch)

fun getTrashedTemplatesQuery(uid: String): Query =
        templates.where("$FIRESTORE_OWNERS.$uid", "<", epoch)

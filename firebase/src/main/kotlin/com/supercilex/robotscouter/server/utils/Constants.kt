package com.supercilex.robotscouter.server.utils

import com.supercilex.robotscouter.server.modules
import com.supercilex.robotscouter.server.utils.types.CollectionReference
import com.supercilex.robotscouter.server.utils.types.DocumentSnapshot
import com.supercilex.robotscouter.server.utils.types.Firestore
import com.supercilex.robotscouter.server.utils.types.Query
import kotlin.js.Date

const val FIRESTORE_OWNERS = "owners"
const val FIRESTORE_NAME = "name"
const val FIRESTORE_EMAIL = "email"
const val FIRESTORE_PHONE_NUMBER = "phoneNumber"
const val FIRESTORE_TYPE = "type"
const val FIRESTORE_VALUE = "value"
const val FIRESTORE_UNIT = "unit"
const val FIRESTORE_SELECTED_VALUE_ID = "selectedValueId"
const val FIRESTORE_TEMPLATE_ID = "templateId"
const val FIRESTORE_NUMBER = "number"
const val FIRESTORE_POSITION = "position"
const val FIRESTORE_SCOUTS = "scouts"
const val FIRESTORE_METRICS = "metrics"
const val FIRESTORE_LAST_LOGIN = "lastLogin"
const val FIRESTORE_TIMESTAMP = "timestamp"
const val FIRESTORE_BASE_TIMESTAMP = "baseTimestamp"
const val FIRESTORE_ACTIVE_TOKENS = "activeTokens"
const val FIRESTORE_PENDING_APPROVALS = "pendingApprovals"
const val FIRESTORE_CONTENT_ID = "contentId"
const val FIRESTORE_SHARE_TYPE = "shareType"

const val FIRESTORE_TEAM_TYPE = 0
const val FIRESTORE_SCOUT_TYPE = 1
const val FIRESTORE_TEMPLATE_TYPE = 2
const val FIRESTORE_SHARE_TOKEN_TYPE = 3

val defaultTemplates: CollectionReference
    get() = modules.firestore.collection("default-templates")
val users: CollectionReference
    get() = modules.firestore.collection("users")
val DocumentSnapshot.userPrefs: CollectionReference
    get() = ref.collection("prefs")
val teams: CollectionReference
    get() = modules.firestore.collection("teams")
val templates: CollectionReference
    get() = modules.firestore.collection("templates")
val deletionQueue: CollectionReference
    get() = modules.firestore.collection("deletion-queue")

private val epoch: Date by lazy { modules.moment(0).toDate() }

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

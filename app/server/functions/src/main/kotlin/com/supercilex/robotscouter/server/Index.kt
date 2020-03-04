package com.supercilex.robotscouter.server

import com.supercilex.robotscouter.common.FIRESTORE_DELETION_QUEUE
import com.supercilex.robotscouter.common.FIRESTORE_DUPLICATE_TEAMS
import com.supercilex.robotscouter.common.FIRESTORE_TEAMS
import com.supercilex.robotscouter.server.functions.deleteUnusedData
import com.supercilex.robotscouter.server.functions.emptyTrash
import com.supercilex.robotscouter.server.functions.initUser
import com.supercilex.robotscouter.server.functions.logUserData
import com.supercilex.robotscouter.server.functions.mergeDuplicateTeams
import com.supercilex.robotscouter.server.functions.populateTeam
import com.supercilex.robotscouter.server.functions.processClientRequest
import com.supercilex.robotscouter.server.functions.sanitizeDeletionRequest
import com.supercilex.robotscouter.server.functions.transferUserData
import com.supercilex.robotscouter.server.functions.updateDefaultTemplates
import com.supercilex.robotscouter.server.functions.updateOwners
import com.supercilex.robotscouter.server.utils.types.functions
import kotlin.js.Json
import kotlin.js.json

external val exports: dynamic

@Suppress("unused") // Used by Cloud Functions
fun main() {
    createManualTriggerFunctions()
    createCronJobFunctions()
    createClientApiFunctions()
    createReactiveFunctions()
    createAuthFunctions()
}

private fun createManualTriggerFunctions() {
    val pubsub = functions.pubsub

    // Trigger: `gcloud beta pubsub topics publish log-user-data '{"uid":"..."}'`
    exports.logUserData = pubsub.topic("log-user-data")
            .onPublish { message, _ -> logUserData(message) }
    exports.updateDefaultTemplates = pubsub.topic("update-default-templates")
            .onPublish { _, _ -> updateDefaultTemplates() }
}

private fun createCronJobFunctions() {
    val pubsub = functions.runWith(json(
            "timeoutSeconds" to 540,
            "memory" to "512MB"
    )).pubsub

    exports.cleanup = pubsub.topic("daily-tick")
            .onPublish { _, _ -> emptyTrash() }
    exports.deleteUnusedData = pubsub.topic("daily-tick")
            .onPublish { _, _ -> deleteUnusedData() }
}

private fun createClientApiFunctions() {
    val https = functions.runWith(json(
            "timeoutSeconds" to 540,
            "memory" to "2GB"
    )).https

    // TODO remove once we stop getting API requests
    exports.emptyTrash = https
            .onCall { data: Array<String>?, context -> emptyTrash(json("ids" to data), context) }
    exports.transferUserData = https
            .onCall { data: Json, context -> transferUserData(data, context) }
    exports.updateOwners = https
            .onCall { data: Json, context -> updateOwners(data, context) }

    exports.clientApi = https
            .onCall { data: Json, context -> processClientRequest(data, context) }
}

private fun createReactiveFunctions() {
    val firestore = functions.runWith(json(
            "timeoutSeconds" to 540,
            "memory" to "1GB"
    )).firestore

    exports.sanitizeDeletionQueue = firestore.document("$FIRESTORE_DELETION_QUEUE/{uid}")
            .onWrite { event, _ -> sanitizeDeletionRequest(event) }
    exports.mergeDuplicateTeams = firestore.document("$FIRESTORE_DUPLICATE_TEAMS/{uid}")
            .onWrite { event, _ -> mergeDuplicateTeams(event) }
    exports.populateTeam = firestore.document("$FIRESTORE_TEAMS/{id}")
            .onWrite { event, _ -> populateTeam(event) }
}

private fun createAuthFunctions() {
    exports.initUser = functions.auth.user().onCreate { user -> initUser(user) }
}

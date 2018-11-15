package com.supercilex.robotscouter.server

import com.supercilex.robotscouter.server.functions.deleteUnusedData
import com.supercilex.robotscouter.server.functions.emptyTrash
import com.supercilex.robotscouter.server.functions.logUserData
import com.supercilex.robotscouter.server.functions.mergeDuplicateTeams
import com.supercilex.robotscouter.server.functions.mergeDuplicateTeamsCompat
import com.supercilex.robotscouter.server.functions.sanitizeDeletionRequest
import com.supercilex.robotscouter.server.functions.updateDefaultTemplates
import com.supercilex.robotscouter.server.functions.updateOwners
import com.supercilex.robotscouter.server.utils.deletionQueue
import com.supercilex.robotscouter.server.utils.duplicateTeams
import com.supercilex.robotscouter.server.utils.jsObject
import com.supercilex.robotscouter.server.utils.teams
import com.supercilex.robotscouter.server.utils.types.admin
import com.supercilex.robotscouter.server.utils.types.functions
import kotlin.js.Json

external fun require(module: String): dynamic
external val exports: dynamic

@Suppress("unused") // Used by Cloud Functions
fun main(args: Array<String>) {
    admin.initializeApp()

    val cleanupRuntime = jsObject {
        timeoutSeconds = 300
        memory = "512MB"
    }

    // Trigger: `gcloud beta pubsub topics publish log-user-data '{"uid":"..."}'`
    exports.logUserData = functions.pubsub.topic("log-user-data")
            .onPublish { message, _ -> logUserData(message) }
    exports.updateDefaultTemplates = functions.pubsub.topic("update-default-templates")
            .onPublish { _, _ -> updateDefaultTemplates() }

    exports.cleanup = functions.runWith(cleanupRuntime).pubsub.topic("daily-tick")
            .onPublish { _, _ -> emptyTrash() }
    exports.deleteUnusedData = functions.runWith(cleanupRuntime).pubsub.topic("daily-tick")
            .onPublish { _, _ -> deleteUnusedData() }
    exports.sanitizeDeletionQueue = functions.firestore.document("${deletionQueue.id}/{uid}")
            .onWrite { event, _ -> sanitizeDeletionRequest(event) }
    exports.emptyTrash = functions.https
            .onCall { data: Array<String>?, context -> emptyTrash(data, context) }

    exports.updateOwners = functions.https
            .onCall { data: Json, context -> updateOwners(data, context) }
    exports.mergeDuplicateTeams = functions.firestore.document("${duplicateTeams.id}/{uid}")
            .onWrite { event, _ -> mergeDuplicateTeams(event) }
    exports.mergeDuplicateTeamsCompat = functions.firestore.document("${teams.id}/{id}")
            .onCreate { snapshot, _ -> mergeDuplicateTeamsCompat(snapshot) }
}

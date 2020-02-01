package com.supercilex.robotscouter.server

import com.supercilex.robotscouter.server.functions.deleteUnusedData
import com.supercilex.robotscouter.server.functions.emptyTrash
import com.supercilex.robotscouter.server.functions.initUser
import com.supercilex.robotscouter.server.functions.logUserData
import com.supercilex.robotscouter.server.functions.mergeDuplicateTeams
import com.supercilex.robotscouter.server.functions.sanitizeDeletionRequest
import com.supercilex.robotscouter.server.functions.transferUserData
import com.supercilex.robotscouter.server.functions.updateDefaultTemplates
import com.supercilex.robotscouter.server.functions.updateOwners
import com.supercilex.robotscouter.server.utils.deletionQueue
import com.supercilex.robotscouter.server.utils.duplicateTeams
import com.supercilex.robotscouter.server.utils.types.admin
import com.supercilex.robotscouter.server.utils.types.functions
import kotlin.js.Json
import kotlin.js.json

external fun require(module: String): dynamic
external val exports: dynamic

@Suppress("unused") // Used by Cloud Functions
fun main() {
    admin.initializeApp()

    // Trigger: `gcloud beta pubsub topics publish log-user-data '{"uid":"..."}'`
    exports.logUserData = functions.pubsub.topic("log-user-data")
            .onPublish { message, _ -> logUserData(message) }
    exports.updateDefaultTemplates = functions.pubsub.topic("update-default-templates")
            .onPublish { _, _ -> updateDefaultTemplates() }

    val cleanupRuntime = json("timeoutSeconds" to 300, "memory" to "512MB")
    exports.emptyTrash = functions.runWith(cleanupRuntime).https
            .onCall { data: Array<String>?, context -> emptyTrash(data, context) }
    exports.cleanup = functions.runWith(cleanupRuntime).pubsub.topic("daily-tick")
            .onPublish { _, _ -> emptyTrash() }
    exports.deleteUnusedData = functions.runWith(cleanupRuntime).pubsub.topic("daily-tick")
            .onPublish { _, _ -> deleteUnusedData() }
    exports.sanitizeDeletionQueue = functions.firestore.document("${deletionQueue.id}/{uid}")
            .onWrite { event, _ -> sanitizeDeletionRequest(event) }

    val transferRuntime = json("timeoutSeconds" to 540, "memory" to "1GB")
    exports.transferUserData = functions.runWith(transferRuntime).https
            .onCall { data: Json, context -> transferUserData(data, context) }
    exports.updateOwners = functions.runWith(transferRuntime).https
            .onCall { data: Json, context -> updateOwners(data, context) }
    exports.mergeDuplicateTeams = functions
            .runWith(json("timeoutSeconds" to 300, "memory" to "256MB"))
            .firestore.document("${duplicateTeams.id}/{uid}")
            .onWrite { event, _ -> mergeDuplicateTeams(event) }

    exports.initUser = functions.auth.user().onCreate { user ->
        initUser(user)
    }
}

package com.supercilex.robotscouter.server

import com.supercilex.robotscouter.server.functions.deleteUnusedData
import com.supercilex.robotscouter.server.functions.emptyTrash
import com.supercilex.robotscouter.server.functions.logUserData
import com.supercilex.robotscouter.server.functions.sanitizeDeletionRequest
import com.supercilex.robotscouter.server.functions.updateDefaultTemplates
import com.supercilex.robotscouter.server.functions.updateOwners
import com.supercilex.robotscouter.server.utils.deletionQueue
import com.supercilex.robotscouter.server.utils.types.admin
import com.supercilex.robotscouter.server.utils.types.functions

external fun require(module: String): dynamic
external val exports: dynamic

@Suppress("unused") // Used by Cloud Functions
fun main(args: Array<String>) {
    admin.initializeApp()

    exports.deleteUnusedData = functions.pubsub.topic("monthly-tick")
            .onPublish { _, _ -> deleteUnusedData() }
    exports.emptyTrash = functions.pubsub.topic("monthly-tick")
            .onPublish { _, _ -> emptyTrash() }
    // Trigger: `gcloud beta pubsub topics publish log-user-data '{"uid":"..."}'`
    exports.logUserData = functions.pubsub.topic("log-user-data")
            .onPublish { message, _ -> logUserData(message) }
    exports.updateDefaultTemplates = functions.pubsub.topic("update-default-templates")
            .onPublish { _, _ -> updateDefaultTemplates() }
    exports.sanitizeDeletionQueue = functions.firestore.document("${deletionQueue.id}/{uid}")
            .onWrite { event, _ -> sanitizeDeletionRequest(event) }
    exports.updateOwners = functions.https
            .onCall { data, context -> updateOwners(data, context) }
}

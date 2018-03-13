package com.supercilex.robotscouter.server

import com.supercilex.robotscouter.server.functions.deleteUnusedData
import com.supercilex.robotscouter.server.functions.emptyTrash
import com.supercilex.robotscouter.server.functions.logUserData
import com.supercilex.robotscouter.server.functions.sanitizeDeletionRequest
import com.supercilex.robotscouter.server.functions.updateDefaultTemplates
import com.supercilex.robotscouter.server.utils.LateinitVal
import com.supercilex.robotscouter.server.utils.Modules
import com.supercilex.robotscouter.server.utils.deletionQueue
import com.supercilex.robotscouter.server.utils.types.DocumentBuilder
import com.supercilex.robotscouter.server.utils.types.TopicBuilder

external fun require(module: String): dynamic
external val exports: dynamic

var modules: Modules by LateinitVal()
    private set

fun main(args: Array<String>) {
    val functions = require("firebase-functions")
    val admin = require("firebase-admin")
    admin.initializeApp(functions.config().firebase)
    val firestore = admin.firestore()
    val moment = require("moment")
    modules = Modules(firestore, moment)

    exports.deleteUnusedData = functions.pubsub.topic("monthly-tick").unsafeCast<TopicBuilder>()
            .onPublish { deleteUnusedData() }
    exports.emptyTrash = functions.pubsub.topic("monthly-tick").unsafeCast<TopicBuilder>()
            .onPublish { emptyTrash() }
    // Trigger: `gcloud beta pubsub topics publish log-user-data '{"uid":"..."}'`
    exports.logUserData = functions.pubsub.topic("log-user-data").unsafeCast<TopicBuilder>()
            .onPublish { logUserData(it) }
    exports.updateDefaultTemplates = functions.pubsub.topic("update-default-templates")
            .unsafeCast<TopicBuilder>()
            .onPublish { updateDefaultTemplates() }
    exports.sanitizeDeletionQueue = functions.firestore.document("${deletionQueue.id}/{uid}")
            .unsafeCast<DocumentBuilder>()
            .onWrite { sanitizeDeletionRequest(it) }
}

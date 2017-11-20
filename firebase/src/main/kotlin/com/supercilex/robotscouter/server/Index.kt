package com.supercilex.robotscouter.server

import com.supercilex.robotscouter.server.utils.FIRESTORE_METRICS
import com.supercilex.robotscouter.server.utils.FIRESTORE_SCOUTS
import com.supercilex.robotscouter.server.utils.LateinitVal
import com.supercilex.robotscouter.server.utils.Modules
import com.supercilex.robotscouter.server.utils.delete
import com.supercilex.robotscouter.server.utils.teams
import com.supercilex.robotscouter.server.utils.types.Event

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

    exports.deleteUnusedData = functions.pubsub.topic("monthly-tick").onPublish { deleteUnusedData() }
    exports.emptyTrash = functions.pubsub.topic("monthly-tick").onPublish { emptyTrash() }
    // Trigger: `gcloud beta pubsub topics publish log-user-data '{"uid":"..."}'`
    exports.logUserData = functions.pubsub.topic("log-user-data").onPublish { event: Event<dynamic> ->
        logUserData(event.data.json.uid)
    }

    exports.deleteTeam = functions.pubsub.topic("delete-team").onPublish { event: Event<dynamic> ->
        teams.doc(event.data.json.id).get().then { team ->
            team.ref.collection(FIRESTORE_SCOUTS).delete {
                it.ref.collection(FIRESTORE_METRICS).delete()
            }.then {
                team.ref.delete()
            }
        }
    }
}

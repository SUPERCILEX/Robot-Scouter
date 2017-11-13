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
    modules = Modules(functions, admin, firestore, moment)

    exports.cleanup = functions.pubsub.topic("monthly-tick").onPublish { cleanup() }
}

external fun require(module: String): dynamic
external val exports: dynamic

fun main(args: Array<String>) {
    val functions = require("firebase-functions")
    val admin = require("firebase-admin")
    admin.initializeApp(functions.config().firebase)

    // TODO https://github.com/VerachadW/kotlin-cloud-functions/blob/master/src/main/kotlin/Index.kt
    exports.cleanup = functions.pubsub.topic("monthly-tick").onPublish {
        console.log("Yay!")
    }
}

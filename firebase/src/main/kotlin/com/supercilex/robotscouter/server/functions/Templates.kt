package com.supercilex.robotscouter.server.functions

import com.supercilex.robotscouter.server.utils.FIRESTORE_METRICS
import com.supercilex.robotscouter.server.utils.FIRESTORE_NAME
import com.supercilex.robotscouter.server.utils.FIRESTORE_TEMPLATE_ID
import com.supercilex.robotscouter.server.utils.FIRESTORE_TIMESTAMP
import com.supercilex.robotscouter.server.utils.FieldValue
import com.supercilex.robotscouter.server.utils.ListItem
import com.supercilex.robotscouter.server.utils.checkbox
import com.supercilex.robotscouter.server.utils.counter
import com.supercilex.robotscouter.server.utils.defaultTemplates
import com.supercilex.robotscouter.server.utils.header
import com.supercilex.robotscouter.server.utils.metrics
import com.supercilex.robotscouter.server.utils.selector
import com.supercilex.robotscouter.server.utils.stopwatch
import com.supercilex.robotscouter.server.utils.text
import com.supercilex.robotscouter.server.utils.types.CollectionReference
import kotlin.js.Json
import kotlin.js.Promise
import kotlin.js.json

fun updateDefaultTemplates() = Promise.all(arrayOf(
        defaultTemplates.updateMatchTemplate(),
        defaultTemplates.updatePitTemplate(),
        defaultTemplates.updateEmptyTemplate()
)).then { Unit }

private fun CollectionReference.updateMatchTemplate() = doc("0").set(json(
        FIRESTORE_TEMPLATE_ID to "0",
        FIRESTORE_NAME to "Match Scout",
        FIRESTORE_TIMESTAMP to FieldValue.serverTimestamp(),
        FIRESTORE_METRICS to matchTemplateMetrics()
).log("Match"))

fun matchTemplateMetrics() = metrics {
    json(
            "a" to header("Scout info"),
            "b" to text("Name"),

            "c" to header("Auto"),
            "d" to checkbox("Crossed baseline"),
            "e" to selector(
                    "Put cube in Switch", "d",
                    ListItem("a", "Didn't attempt"),
                    ListItem("b", "Attempted"),
                    ListItem("c", "Successful"),
                    ListItem("d", "Unknown")
            ),
            "f" to selector(
                    "Put cube in Scale", "d",
                    ListItem("a", "Didn't attempt"),
                    ListItem("b", "Attempted"),
                    ListItem("c", "Successful"),
                    ListItem("d", "Unknown")
            ),
            "g" to checkbox("Picked up cube"),

            "h" to header("Teleop"),
            "i" to counter("Cubes put in the Switch"),
            "j" to counter("Cubes put in the Scale"),
            "k" to counter("Cubes put in the Exchange"),
            "l" to stopwatch("Cycle time"),
            "m" to checkbox("Climbed"),

            "n" to header("Post-game"),
            "o" to checkbox("Robot broke"),
            "p" to text("Other")
    )
}

private fun CollectionReference.updatePitTemplate() = doc("1").set(json(
        FIRESTORE_TEMPLATE_ID to "1",
        FIRESTORE_NAME to "Pit Scout",
        FIRESTORE_TIMESTAMP to FieldValue.serverTimestamp(),
        FIRESTORE_METRICS to pitTemplateMetrics()
).log("Pit"))

fun pitTemplateMetrics() = metrics {
    json(
            "a" to header("Scout info"),
            "b" to text("Name"),

            "c" to header("Hardware"),
            "d" to selector(
                    "Drivetrain", "d",
                    ListItem("a", "Standard 6/8 wheel"),
                    ListItem("b", "Swerve"),
                    ListItem("c", "Omni/Mecanum"),
                    ListItem("d", "Unknown")
            ),
            "e" to text("How does it climb?"),
            "f" to checkbox("Can we climb on them?"),
            "g" to text("If so, where's the bar?"),
            "h" to counter("Subjective quality assessment", 3, "⭐"),

            "i" to header("Strategy"),
            "j" to selector(
                    "Autonomous", "a",
                    ListItem("a", "None"),
                    ListItem("b", "Drive"),
                    ListItem("c", "Switch"),
                    ListItem("d", "Scale"),
                    ListItem("e", "Switch + Scale")
            ),
            "m" to selector(
                    "Where can they place cubes?", "a",
                    ListItem("a", "Just the exchange zone"),
                    ListItem("b", "Switch"),
                    ListItem("c", "Scale"),
                    ListItem("c", "Switch and scale")
            ),
            "k" to text("What is special about your robot or something you want us to know?"),
            "l" to text("Other")
    )
}

private fun CollectionReference.updateEmptyTemplate() = doc("2").set(json(
        FIRESTORE_TEMPLATE_ID to "2",
        FIRESTORE_NAME to "Blank Scout",
        FIRESTORE_TIMESTAMP to FieldValue.serverTimestamp()
).log("Blank"))

private fun Json.log(name: String) = apply { console.log("$name: ${JSON.stringify(this)}") }

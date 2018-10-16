package com.supercilex.robotscouter.server.functions

import com.supercilex.robotscouter.common.FIRESTORE_METRICS
import com.supercilex.robotscouter.common.FIRESTORE_NAME
import com.supercilex.robotscouter.common.FIRESTORE_TEMPLATE_ID
import com.supercilex.robotscouter.common.FIRESTORE_TIMESTAMP
import com.supercilex.robotscouter.server.utils.checkbox
import com.supercilex.robotscouter.server.utils.counter
import com.supercilex.robotscouter.server.utils.defaultTemplates
import com.supercilex.robotscouter.server.utils.header
import com.supercilex.robotscouter.server.utils.metrics
import com.supercilex.robotscouter.server.utils.selector
import com.supercilex.robotscouter.server.utils.stopwatch
import com.supercilex.robotscouter.server.utils.text
import com.supercilex.robotscouter.server.utils.types.CollectionReference
import com.supercilex.robotscouter.server.utils.types.FieldValues
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asPromise
import kotlinx.coroutines.async
import kotlinx.coroutines.await
import kotlinx.coroutines.awaitAll
import kotlin.js.Json
import kotlin.js.Promise
import kotlin.js.json

fun updateDefaultTemplates(): Promise<*>? {
    return GlobalScope.async {
        val match = async { defaultTemplates.updateMatchTemplate() }
        val pit = async { defaultTemplates.updatePitTemplate() }
        val empty = async { defaultTemplates.updateEmptyTemplate() }

        awaitAll(match, pit, empty)
    }.asPromise()
}

private suspend fun CollectionReference.updateMatchTemplate() {
    doc("0").set(json(
            FIRESTORE_TEMPLATE_ID to "0",
            FIRESTORE_NAME to "Match Scout",
            FIRESTORE_TIMESTAMP to FieldValues.serverTimestamp(),
            FIRESTORE_METRICS to matchTemplateMetrics()
    ).log("Match")).await()
}

fun matchTemplateMetrics() = metrics {
    header("a", "Scout info")
    text("b", "Name")

    header("c", "Auto")
    checkbox("d", "Crossed baseline")
    selector("e", "Put cube in Switch") {
        +Item("a", "Didn't attempt")
        +Item("b", "Attempted")
        +Item("c", "Successful")
        +Item("d", "Unknown", true)
    }
    selector("f", "Put cube in Scale") {
        +Item("a", "Didn't attempt")
        +Item("b", "Attempted")
        +Item("c", "Successful")
        +Item("d", "Unknown", true)
    }
    checkbox("g", "Picked up cube")

    header("h", "Teleop")
    counter("i", "Cubes put in the Switch")
    counter("j", "Cubes put in the Scale")
    counter("k", "Cubes put in the Exchange")
    stopwatch("l", "Cycle time")
    checkbox("m", "Climbed")

    header("n", "Post-game")
    checkbox("o", "Robot broke")
    text("p", "Other")
}

private suspend fun CollectionReference.updatePitTemplate() {
    doc("1").set(json(
            FIRESTORE_TEMPLATE_ID to "1",
            FIRESTORE_NAME to "Pit Scout",
            FIRESTORE_TIMESTAMP to FieldValues.serverTimestamp(),
            FIRESTORE_METRICS to pitTemplateMetrics()
    ).log("Pit")).await()
}

fun pitTemplateMetrics() = metrics {
    header("a", "Scout info")
    text("b", "Name")

    header("c", "Hardware")
    selector("d", "What's their drivetrain?") {
        +Item("d", "Unknown")
        +Item("a", "Standard 6/8 wheel")
        +Item("b", "Swerve")
        +Item("c", "Omni/Mecanum")
        +Item("e", "Other")
    }
    text("n", "If other, please specify")
    checkbox("o", "Does it climb?")
    text("e", "How does it climb?")
    checkbox("f", "Can they help us climb?")
    text("g", "If so, how?")
    counter("h", "Subjective quality assessment (?/5)") {
        count = 0
        unit = "⭐"
    }

    header("i", "Strategy")
    selector("j", "What's their autonomous?") {
        +Item("a", "None")
        +Item("b", "Drive")
        +Item("c", "Switch")
        +Item("d", "Scale")
        +Item("e", "Switch OR Scale")
        +Item("f", "Switch AND Scale")
    }
    selector("m", "Where can they place cubes?") {
        +Item("a", "Just the exchange zone")
        +Item("b", "Switch")
        +Item("c", "Scale")
        +Item("d", "Switch and scale")
    }
    text("k", "What is special about your robot or something you want us to know?")
    text("l", "Other")
}

private suspend fun CollectionReference.updateEmptyTemplate() {
    doc("2").set(json(
            FIRESTORE_TEMPLATE_ID to "2",
            FIRESTORE_NAME to "Blank Scout",
            FIRESTORE_TIMESTAMP to FieldValues.serverTimestamp()
    ).log("Blank")).await()
}

private fun Json.log(name: String) = apply { console.log("$name: ${JSON.stringify(this)}") }

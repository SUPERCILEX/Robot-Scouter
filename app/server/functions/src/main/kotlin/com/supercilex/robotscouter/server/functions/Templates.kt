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

/**
 * This function and [pitTemplateMetrics] update the default templates server-side without requiring
 * a full app update. To improve the templates or update them for a new game, follow these
 * instructions.
 *
 * ## Creating a new template for a new game
 *
 * 1. Delete the existing metrics
 * 1. Make sure to update _both_ the match and pit scouting templates (the pit template likely won't
 *    require much work)
 * 1. Increment the letter for each new metric added (e.g. 'a' -> 'b' -> 'c')
 *      - Should the template exceed 26 items, start with `aa` -> 'ab' -> 'ac"
 * 1. Always start with a header and space the metrics in code by grouping
 * 1. **For examples of correct metric DSL usage**, take a look at the current template or the Git
 *    log for past templates
 *
 * ## Improving the current year's template
 *
 * 1. When moving metrics around, keep their letter IDs the same
 * 1. When inserting a metric, don't change the IDs of surrounding metrics; just pick an unused ID
 * 1. Minimize deletions at all costs since that makes data analysis harder
 */
fun matchTemplateMetrics() = metrics {
    header("a", "Scout info")
    text("b", "Name")

    header("c", "Sandstorm")
    selector("d", "Starting Location") {
        +Item("a", "HAB Level 2")
        +Item("b", "HAB Level 1")
    }
    checkbox("e", "Successfully crossed HAB line")
    counter("f", "Panels on Cargo Ship")
    counter("g", "Panels on Low Rocket Hatches")
    counter("h", "Panels on Middle/High Rocket Hatches")
    counter("i", "Cargo in Cargo Ship")
    counter("j", "Cargo in Low Rocket Bays")
    counter("k", "Cargo in Middle/High Rocket Bays")

    header("l", "Teleop")
    counter("m", "Panels on Cargo Ship")
    counter("n", "Panels on Low Rocket Hatches")
    counter("o", "Panels on Middle/High Rocket Hatches")
    counter("p", "Cargo in Cargo Ship")
    counter("q", "Cargo in Low Rocket Bays")
    counter("r", "Cargo in Middle/High Rocket Bays")
    stopwatch("s", "Cycle Time")
    selector("t", "Endgame Location") {
        +Item("a", "HAB Level 1")
        +Item("b", "HAB Level 2")
        +Item("c", "HAB Level 3")
        +Item("d", "Not on HAB")
    }

    header("u", "Post-game")
    checkbox("v", "Robot Broke")
    text("w", "Other")
}

/** @see [matchTemplateMetrics] */
fun pitTemplateMetrics() = metrics {
    header("a", "Scout info")
    text("b", "Name")

    header("c", "Hardware")
    selector("d", "What's their drivetrain?") {
        +Item("a", "Unknown")
        +Item("b", "Standard 6/8 wheel")
        +Item("c", "Swerve")
        +Item("d", "Omni/Mecanum")
        +Item("e", "Other")
    }
    text("e", "If other, please specify")
    checkbox("f", "Do they have a Hatch Panel ground intake?")
    checkbox("g", "Do they have a Cargo ground intake?")

    header("h", "Sandstorm Strategy")
    selector("i", "From where does their robot start from?") {
        +Item("a", "HAB Level 1")
        +Item("b", "HAB Level 2")
    }
    selector("j", "How does their robot move during the Sandstorm?") {
        +Item("a", "Autonomous Code")
        +Item("b", "Driver Control")
        +Item("c", "No Sandstorm Movement")
        +Item("d", "Hybrid")
    }
    selector("k", "Where can they place Hatch Panels during the Sandstorm?") {
        +Item("a", "Can't place Panels durin the Sandstorm")
        +Item("b", "Cargo Ship Only")
        +Item("c", "Cargo Ship and Low Rocket Hatches")
        +Item("d", "Cargo Ship and Low/Middle Rocket Hatches")
        +Item("e", "Everywhere")
        +Item("f", "Other")
    }
    text("l", "If other, please specify")
    selector("m", "Where can they place Cargo during the Sandstorm?") {
        +Item("a", "Can't place Cargo during the Sandstorm")
        +Item("b", "Cargo Ship Only")
        +Item("c", "Cargo Ship and Low Rocket Bays")
        +Item("d", "Cargo Ship and Low/Middle Rocket Bays")
        +Item("e", "Everywhere")
        +Item("f", "Other")
    }
    text("n", "If other, please specify")

    header("o", "Teleop Strategy")
    selector("p", "Where can they place Hatch Panels during Teleop?") {
        +Item("a", "Can't place Panels during Teleop")
        +Item("b", "Cargo Ship Only")
        +Item("c", "Cargo Ship and Low Rocket Hatches")
        +Item("d", "Cargo Ship and Low/Middle Rocket Hatches")
        +Item("e", "Everywhere")
        +Item("f", "Other")
    }
    text("q", "If other, please specify")
    selector("r", "Where do they place Cargo during Teleop?") {
        +Item("a", "Can't place Cargo during Teleop")
        +Item("b", "Cargo Ship Only")
        +Item("c", "Cargo Ship and Low Rocket Bays")
        +Item("d", "Cargo Ship and Low/Middle Rocket Bays")
        +Item("e", "Everywhere")
        +Item("f", "Other")
    }
    text("s", "If other, please specify")
    selector("t", "Where does their robot end the game?") {
        +Item("a", "Off the HAB Platform")
        +Item("b", "Only HAB Level 1")
        +Item("c", "Only HAB Level 2")
        +Item("d", "Only HAB Level 3")
        +Item("e", "HAB Level 1 OR 2")
        +Item("f", "HAB Level 1 OR 3")
        +Item("g", "HAB Level 1, 2, OR 3")
    }
    checkbox("u", "Can they help another robot climb? ")

    header("v", "Other")
    counter("w", "Subjective quality assessment (?/5)") {
        count = 0
        unit = "⭐"
    }
    text("x", "What is something special you want us to know about your robot?")
    text("y", "Other Comments")
}

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

private suspend fun CollectionReference.updatePitTemplate() {
    doc("1").set(json(
            FIRESTORE_TEMPLATE_ID to "1",
            FIRESTORE_NAME to "Pit Scout",
            FIRESTORE_TIMESTAMP to FieldValues.serverTimestamp(),
            FIRESTORE_METRICS to pitTemplateMetrics()
    ).log("Pit")).await()
}

private suspend fun CollectionReference.updateEmptyTemplate() {
    doc("2").set(json(
            FIRESTORE_TEMPLATE_ID to "2",
            FIRESTORE_NAME to "Blank Scout",
            FIRESTORE_TIMESTAMP to FieldValues.serverTimestamp()
    ).log("Blank")).await()
}

private fun Json.log(name: String) = apply { console.log("$name: ${JSON.stringify(this)}") }

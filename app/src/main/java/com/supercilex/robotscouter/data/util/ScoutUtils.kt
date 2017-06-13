package com.supercilex.robotscouter.data.util

import android.os.Bundle
import android.text.TextUtils
import com.firebase.ui.database.SnapshotParser
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.crash.FirebaseCrash
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener
import com.supercilex.robotscouter.data.model.BOOLEAN
import com.supercilex.robotscouter.data.model.HEADER
import com.supercilex.robotscouter.data.model.LIST
import com.supercilex.robotscouter.data.model.Metric
import com.supercilex.robotscouter.data.model.NUMBER
import com.supercilex.robotscouter.data.model.STOPWATCH
import com.supercilex.robotscouter.data.model.TEXT
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.Constants
import com.supercilex.robotscouter.util.FIREBASE_METRICS
import com.supercilex.robotscouter.util.FIREBASE_NAME
import com.supercilex.robotscouter.util.FIREBASE_SCOUTS
import com.supercilex.robotscouter.util.FIREBASE_SCOUT_INDICES
import com.supercilex.robotscouter.util.FIREBASE_SCOUT_TEMPLATES
import com.supercilex.robotscouter.util.FIREBASE_SELECTED_VALUE_KEY
import com.supercilex.robotscouter.util.FIREBASE_TYPE
import com.supercilex.robotscouter.util.FIREBASE_UNIT
import com.supercilex.robotscouter.util.FIREBASE_VALUE
import com.supercilex.robotscouter.util.logAddScoutEvent

val SCOUT_KEY = "scout_key"
val METRIC_PARSER = SnapshotParser<Metric<*>> { snapshot ->
    val metric: Metric<*>
    val type = snapshot.child(FIREBASE_TYPE).getValue(Int::class.java) ?:
            // This appears to happen in the in-between state when the metric has been half copied.
            return@SnapshotParser Metric.Header("Sanity check failed. Please report: bit.ly/RSGitHub.")


    val name = snapshot.child(FIREBASE_NAME).getValue(String::class.java) ?: ""
    val value = snapshot.child(FIREBASE_VALUE)

    when (type) {
        BOOLEAN -> metric = Metric.Boolean(name, value.getValue(Boolean::class.java)!!)
        NUMBER -> {
            metric = Metric.Number(
                    name,
                    value.getValue(object : GenericTypeIndicator<Long>() {})!!,
                    snapshot.child(FIREBASE_UNIT).getValue(String::class.java))
        }
        TEXT -> metric = Metric.Text(name, value.getValue(String::class.java))
        LIST -> {
            metric = Metric.List(
                    name,
                    value.children.associateBy({ it.key }, { it.getValue(String::class.java) ?: "" }),
                    snapshot.child(FIREBASE_SELECTED_VALUE_KEY).getValue(String::class.java))
        }
        STOPWATCH -> {
            metric = Metric.Stopwatch(
                    name,
                    value.children.map { it.getValue(Long::class.java)!! })
        }
        HEADER -> metric = Metric.Header(name)
        else -> throw IllegalStateException("Unknown metric type: $type")
    }

    metric.ref = snapshot.ref
    metric
}

fun getScoutMetricsRef(key: String): DatabaseReference =
        FIREBASE_SCOUTS.child(key).child(FIREBASE_METRICS)

fun getScoutKeyBundle(key: String?): Bundle {
    val args = Bundle()
    args.putString(SCOUT_KEY, key)
    return args
}

fun getScoutKey(bundle: Bundle): String? {
    return bundle.getString(SCOUT_KEY)
}

fun getScoutIndicesRef(teamKey: String): DatabaseReference {
    return FIREBASE_SCOUT_INDICES.child(teamKey)
}

fun addScout(team: Team): String {
    logAddScoutEvent(team.number)

    val indexRef = getScoutIndicesRef(team.key).push()
    indexRef.setValue(System.currentTimeMillis())
    val scoutRef = getScoutMetricsRef(indexRef.key)

    if (TextUtils.isEmpty(team.templateKey)) {
        FirebaseCopier.copyTo(Constants.sDefaultTemplate, scoutRef)
    } else {
        FirebaseCopier(FIREBASE_SCOUT_TEMPLATES.child(team.templateKey), scoutRef)
                .performTransformation()
    }

    return indexRef.key
}

fun deleteScout(teamKey: String, scoutKey: String) {
    FIREBASE_SCOUTS.child(scoutKey).removeValue()
    getScoutIndicesRef(teamKey).child(scoutKey).removeValue()
}

fun deleteAllScouts(teamKey: String): Task<Void> {
    val deleteTask = TaskCompletionSource<Void>()
    getScoutIndicesRef(teamKey).addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            for (keySnapshot in snapshot.children) {
                FIREBASE_SCOUTS.child(keySnapshot.key).removeValue()
                keySnapshot.ref.removeValue()
            }
            deleteTask.setResult(null)
        }

        override fun onCancelled(error: DatabaseError) {
            deleteTask.setException(error.toException())
            FirebaseCrash.report(error.toException())
        }
    })
    return deleteTask.task
}

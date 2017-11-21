package com.supercilex.robotscouter.util.data.model

import android.arch.lifecycle.LiveData
import com.firebase.ui.firestore.ObservableSnapshotArray
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.supercilex.robotscouter.data.model.Scout
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.data.model.TemplateType
import com.supercilex.robotscouter.util.AsyncTaskExecutor
import com.supercilex.robotscouter.util.FIRESTORE_METRICS
import com.supercilex.robotscouter.util.FIRESTORE_NAME
import com.supercilex.robotscouter.util.FIRESTORE_SCOUTS
import com.supercilex.robotscouter.util.FIRESTORE_TIMESTAMP
import com.supercilex.robotscouter.util.async
import com.supercilex.robotscouter.util.data.firestoreBatch
import com.supercilex.robotscouter.util.data.observeOnDataChanged
import com.supercilex.robotscouter.util.data.observeOnce
import com.supercilex.robotscouter.util.data.scoutParser
import com.supercilex.robotscouter.util.defaultTemplates
import com.supercilex.robotscouter.util.logAddScoutEvent
import com.supercilex.robotscouter.util.logFailures
import java.util.Date
import kotlin.math.abs

fun Team.getScoutsRef() = ref.collection(FIRESTORE_SCOUTS)

fun Team.getScoutsQuery(direction: Query.Direction = Query.Direction.DESCENDING): Query =
        FIRESTORE_TIMESTAMP.let {
            getScoutsRef().whereGreaterThanOrEqualTo(it, Date(0)).orderBy(it, direction)
        }

fun Team.getScoutMetricsRef(id: String) = getScoutsRef().document(id).collection(FIRESTORE_METRICS)

fun Team.addScout(
        overrideId: String?,
        existingScouts: LiveData<ObservableSnapshotArray<Scout>>
): String {
    val templateId = overrideId ?: templateId
    val scoutRef = getScoutsRef().document()

    scoutRef.set(Scout(scoutRef.id, templateId))
    (TemplateType.coerce(templateId)?.let { type ->
        defaultTemplates.document(type.id.toString()).get().continueWith(
                AsyncTaskExecutor, Continuation<DocumentSnapshot, String?> {
            val scout = scoutParser.parseSnapshot(it.result)

            firestoreBatch {
                scout.metrics.forEach {
                    set(getScoutMetricsRef(scoutRef.id).document(it.ref.id), it)
                }
            }.logFailures()

            scout.name
        })
    } ?: run {
        getTemplateRef(templateId).collection(FIRESTORE_METRICS).get().continueWithTask(
                AsyncTaskExecutor, Continuation<QuerySnapshot, Task<Void>> {
            firestoreBatch {
                it.result.documents.associate { it.id to it.data }.forEach {
                    set(getScoutMetricsRef(scoutRef.id).document(it.key), it.value)
                }
            }
        }).logFailures()

        getTemplateRef(templateId).get().continueWith {
            if (it.result.exists()) {
                scoutParser.parseSnapshot(it.result).name
            } else {
                null
            }
        }
    }).logFailures().addOnCompleteListener(AsyncTaskExecutor, OnCompleteListener {
        val templateName = it.result!! // Blow up if we failed so as not to wastefully query for scouts
        val nExistingTemplates = Tasks.await(existingScouts.observeOnDataChanged().observeOnce {
            async {
                it.map { it.templateId }.groupBy { it }[templateId]!!.size
            }
        })

        scoutRef.update(FIRESTORE_NAME, "$templateName $nExistingTemplates").logFailures()
    })

    logAddScoutEvent(this, id, templateId)
    return scoutRef.id
}

fun Team.trashScout(id: String) {
    getScoutsRef().document(id).get().continueWithTask(
            AsyncTaskExecutor, Continuation<DocumentSnapshot, Task<Void>> {
        val snapshot = it.result
        val oppositeDate = Date(-abs(snapshot.getDate(FIRESTORE_TIMESTAMP).time))
        snapshot.reference.update(FIRESTORE_TIMESTAMP, oppositeDate)
    }).logFailures()
}

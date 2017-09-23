package com.supercilex.robotscouter.util.data.model

import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.QuerySnapshot
import com.supercilex.robotscouter.data.model.Scout
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.data.model.isNativeTemplateType
import com.supercilex.robotscouter.util.AsyncTaskExecutor
import com.supercilex.robotscouter.util.CrashLogger
import com.supercilex.robotscouter.util.FIRESTORE_DEFAULT_TEMPLATES
import com.supercilex.robotscouter.util.FIRESTORE_METRICS
import com.supercilex.robotscouter.util.FIRESTORE_SCOUTS
import com.supercilex.robotscouter.util.data.SCOUT_PARSER
import com.supercilex.robotscouter.util.data.delete
import com.supercilex.robotscouter.util.logAddScoutEvent

fun Team.getScoutRef() = ref.collection(FIRESTORE_SCOUTS)

fun Team.getScoutMetricsRef(id: String) = getScoutRef().document(id).collection(FIRESTORE_METRICS)

fun Team.addScout(overrideId: String? = null): String {
    val templateId = overrideId ?: templateId

    val scoutRef = getScoutRef().document()
    scoutRef.set(Scout(scoutRef.id, templateId))

    if (isNativeTemplateType(templateId)) {
        FIRESTORE_DEFAULT_TEMPLATES.get().addOnSuccessListener(
                AsyncTaskExecutor, OnSuccessListener {
            it.map {
                SCOUT_PARSER.parseSnapshot(it)
            }.find { it.id == templateId }!!.metrics.forEach {
                getScoutMetricsRef(scoutRef.id).document(it.ref.id).set(it)
            }
        })
    } else {
        getTemplateRef(templateId).collection(FIRESTORE_METRICS).get().addOnSuccessListener(
                AsyncTaskExecutor, OnSuccessListener {
            it.documents.associate { it.id to it.data }.forEach {
                getScoutMetricsRef(scoutRef.id).document(it.key).set(it.value)
            }
        }).addOnFailureListener(CrashLogger)
    }

    logAddScoutEvent(this, templateId)
    return scoutRef.id
}

fun Team.deleteScout(id: String): Task<Void> = getScoutMetricsRef(id).delete().continueWithTask {
    getScoutRef().document(id).delete()
}

fun Team.deleteAllScouts(): Task<Void> = getScoutRef().get().continueWithTask(
        AsyncTaskExecutor, Continuation<QuerySnapshot, Task<Void>> {
    Tasks.whenAll(it.result.map { deleteScout(it.id) })
})

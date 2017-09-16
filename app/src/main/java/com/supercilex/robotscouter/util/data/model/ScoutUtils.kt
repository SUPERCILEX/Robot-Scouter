package com.supercilex.robotscouter.util.data.model

import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Tasks
import com.supercilex.robotscouter.data.model.Scout
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.data.model.isNativeTemplateType
import com.supercilex.robotscouter.util.AsyncTaskExecutor
import com.supercilex.robotscouter.util.FIRESTORE_METRICS
import com.supercilex.robotscouter.util.FIRESTORE_SCOUTS
import com.supercilex.robotscouter.util.data.DefaultTemplatesLiveData
import com.supercilex.robotscouter.util.data.delete
import com.supercilex.robotscouter.util.data.observeOnDataChanged
import com.supercilex.robotscouter.util.data.observeOnce
import com.supercilex.robotscouter.util.logAddScoutEvent

fun Team.getScoutRef() = ref.collection(FIRESTORE_SCOUTS)

fun Team.getScoutMetricsRef(id: String) = getScoutRef().document(id).collection(FIRESTORE_METRICS)

fun Team.addScout(overrideId: String? = null): String {
    val templateId = overrideId ?: templateId

    val scoutRef = getScoutRef().document()
    scoutRef.set(Scout(scoutRef.id, templateId))

    if (isNativeTemplateType(templateId)) {
        DefaultTemplatesLiveData.observeOnDataChanged().observeOnce {
            it.find { it.id == templateId }!!.metrics.forEach {
                getScoutMetricsRef(scoutRef.id).document(it.ref.id).set(it)
            }
            Tasks.forResult(null)
        }
    } else {
        getTemplateRef(templateId).collection(FIRESTORE_METRICS).get()
                .addOnSuccessListener(AsyncTaskExecutor, OnSuccessListener {
                    it.documents.associate { it.id to it.data }.forEach {
                        getScoutMetricsRef(scoutRef.id).document(it.key).set(it.value)
                    }
                })
    }

    logAddScoutEvent(this, templateId)
    return scoutRef.id
}

fun Team.deleteScout(id: String) {
    getScoutRef().document(id).delete()
    getScoutMetricsRef(id).delete()
}

fun Team.deleteAllScouts() {
    ref.delete()
    getScoutRef().delete().addOnSuccessListener {
        for (snapshot in it) deleteScout(snapshot.id)
    }
}

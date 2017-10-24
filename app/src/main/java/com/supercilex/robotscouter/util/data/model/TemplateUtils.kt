package com.supercilex.robotscouter.util.data.model

import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.data.model.Scout
import com.supercilex.robotscouter.data.model.TemplateType
import com.supercilex.robotscouter.util.AsyncTaskExecutor
import com.supercilex.robotscouter.util.CrashLogger
import com.supercilex.robotscouter.util.FIRESTORE_METRICS
import com.supercilex.robotscouter.util.FIRESTORE_OWNERS
import com.supercilex.robotscouter.util.data.batch
import com.supercilex.robotscouter.util.data.delete
import com.supercilex.robotscouter.util.data.firestoreBatch
import com.supercilex.robotscouter.util.data.scoutParser
import com.supercilex.robotscouter.util.defaultTemplates
import com.supercilex.robotscouter.util.logAddTemplateEvent
import com.supercilex.robotscouter.util.templates
import com.supercilex.robotscouter.util.uid
import java.util.Date

fun getTemplatesQuery(direction: Query.Direction = Query.Direction.ASCENDING): Query =
        "$FIRESTORE_OWNERS.${uid!!}".let {
            templates.whereGreaterThanOrEqualTo(it, Date(0)).orderBy(it, direction)
        }

fun getTemplateRef(id: String) = templates.document(id)

fun getTemplateMetricsRef(id: String) = getTemplateRef(id).collection(FIRESTORE_METRICS)

fun addTemplate(type: TemplateType): String {
    val ref = templates.document()
    val id = ref.id

    ref.batch {
        val scout = Scout(id, id)
        set(it, scout)
        update(it, FIRESTORE_OWNERS, mapOf(uid!! to scout.timestamp))
    }

    defaultTemplates.get().continueWithTask(
            AsyncTaskExecutor, Continuation<QuerySnapshot, Task<Void>> {
        firestoreBatch {
            it.result.map {
                scoutParser.parseSnapshot(it)
            }.find { it.id == type.id.toString() }!!.metrics.forEach {
                set(getTemplateMetricsRef(id).document(it.ref.id), it)
            }
        }
    }).addOnFailureListener(CrashLogger)

    logAddTemplateEvent(id)
    return id
}

fun Scout.getTemplateName(index: Int): String =
        name ?: RobotScouter.INSTANCE.getString(R.string.template_tab_default_title, index + 1)

fun deleteTemplate(id: String) {
    getTemplateMetricsRef(id).delete().addOnSuccessListener {
        getTemplateRef(id).delete()
    }
}

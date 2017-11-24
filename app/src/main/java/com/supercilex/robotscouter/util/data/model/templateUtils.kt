package com.supercilex.robotscouter.util.data.model

import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.data.model.Scout
import com.supercilex.robotscouter.data.model.TemplateType
import com.supercilex.robotscouter.util.AsyncTaskExecutor
import com.supercilex.robotscouter.util.FIRESTORE_METRICS
import com.supercilex.robotscouter.util.FIRESTORE_OWNERS
import com.supercilex.robotscouter.util.FIRESTORE_TIMESTAMP
import com.supercilex.robotscouter.util.data.batch
import com.supercilex.robotscouter.util.data.firestoreBatch
import com.supercilex.robotscouter.util.data.scoutParser
import com.supercilex.robotscouter.util.defaultTemplates
import com.supercilex.robotscouter.util.logAddTemplate
import com.supercilex.robotscouter.util.logFailures
import com.supercilex.robotscouter.util.templates
import com.supercilex.robotscouter.util.uid
import java.util.Date
import kotlin.math.abs

fun getTemplatesQuery(direction: Query.Direction = Query.Direction.ASCENDING): Query =
        "$FIRESTORE_OWNERS.${uid!!}".let {
            templates.whereGreaterThanOrEqualTo(it, Date(0)).orderBy(it, direction)
        }

fun getTemplateRef(id: String) = templates.document(id)

fun getTemplateMetricsRef(id: String) = getTemplateRef(id).collection(FIRESTORE_METRICS)

fun addTemplate(type: TemplateType): String {
    val ref = templates.document()
    val id = ref.id

    logAddTemplate(id, type)
    ref.batch {
        val scout = Scout(id, id)
        set(it, scout)
        update(it, FIRESTORE_OWNERS, mapOf(uid!! to scout.timestamp))
    }

    defaultTemplates.document(type.id.toString()).get().continueWithTask(
            AsyncTaskExecutor, Continuation<DocumentSnapshot, Task<Void>> {
        firestoreBatch {
            scoutParser.parseSnapshot(it.result).metrics.forEach {
                set(getTemplateMetricsRef(id).document(it.ref.id), it)
            }
        }
    }).logFailures()

    return id
}

fun Scout.getTemplateName(index: Int): String =
        name ?: RobotScouter.INSTANCE.getString(R.string.template_tab_default_title, index + 1)

fun trashTemplate(id: String) {
    getTemplateRef(id).get().continueWithTask(
            AsyncTaskExecutor, Continuation<DocumentSnapshot, Task<Void>> {
        val snapshot = it.result
        val oppositeDate = Date(-abs(snapshot.getDate(FIRESTORE_TIMESTAMP).time))
        snapshot.reference.update("$FIRESTORE_OWNERS.${uid!!}", oppositeDate)
    }).logFailures()
}

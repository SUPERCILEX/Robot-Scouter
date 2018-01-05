package com.supercilex.robotscouter.util.data.model

import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.appindexing.Action
import com.google.firebase.appindexing.FirebaseAppIndex
import com.google.firebase.appindexing.FirebaseUserActions
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
import com.supercilex.robotscouter.util.data.getTemplateIndexable
import com.supercilex.robotscouter.util.data.getTemplateLink
import com.supercilex.robotscouter.util.data.scoutParser
import com.supercilex.robotscouter.util.defaultTemplates
import com.supercilex.robotscouter.util.log
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
    FirebaseAppIndex.getInstance()
            .update(getTemplateIndexable(id, "Template"))
            .logFailures()
    FirebaseUserActions.getInstance().end(
            Action.Builder(Action.Builder.ADD_ACTION)
                    .setObject("Template", getTemplateLink(id))
                    .setActionStatus(Action.Builder.STATUS_TYPE_COMPLETED)
                    .build()
    ).logFailures()

    ref.batch {
        val scout = Scout(id, id)
        set(it, scout)
        update(it, FIRESTORE_OWNERS, mapOf(uid!! to scout.timestamp))
    }.logFailures()

    defaultTemplates.document(type.id.toString()).log().get().continueWithTask(
            AsyncTaskExecutor, Continuation<DocumentSnapshot, Task<Void>> {
        firestoreBatch {
            scoutParser.parseSnapshot(it.result).metrics.forEach {
                set(getTemplateMetricsRef(id).document(it.ref.id).log(), it)
            }
        }
    }).logFailures()

    return id
}

fun Scout.getTemplateName(index: Int): String =
        name ?: RobotScouter.getString(R.string.template_tab_default_title, index + 1)

fun trashTemplate(id: String) {
    FirebaseAppIndex.getInstance().remove(getTemplateLink(id)).logFailures()
    getTemplateRef(id).log().get().continueWithTask(
            AsyncTaskExecutor, Continuation<DocumentSnapshot, Task<Void>> {
        val snapshot = it.result
        val oppositeDate = Date(-abs(snapshot.getDate(FIRESTORE_TIMESTAMP).time))
        snapshot.reference.log().update("$FIRESTORE_OWNERS.${uid!!}", oppositeDate)
    }).logFailures()
}

package com.supercilex.robotscouter.core.data.model

import com.google.firebase.appindexing.Action
import com.google.firebase.appindexing.FirebaseAppIndex
import com.google.firebase.appindexing.FirebaseUserActions
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.supercilex.robotscouter.common.FIRESTORE_METRICS
import com.supercilex.robotscouter.common.FIRESTORE_OWNERS
import com.supercilex.robotscouter.common.FIRESTORE_TEMPLATE_ID
import com.supercilex.robotscouter.common.FIRESTORE_TIMESTAMP
import com.supercilex.robotscouter.core.CrashLogger
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.asTask
import com.supercilex.robotscouter.core.await
import com.supercilex.robotscouter.core.data.QueuedDeletion
import com.supercilex.robotscouter.core.data.R
import com.supercilex.robotscouter.core.data.defaultTemplateId
import com.supercilex.robotscouter.core.data.defaultTemplatesRef
import com.supercilex.robotscouter.core.data.firestoreBatch
import com.supercilex.robotscouter.core.data.getTemplateIndexable
import com.supercilex.robotscouter.core.data.getTemplateLink
import com.supercilex.robotscouter.core.data.logAddTemplate
import com.supercilex.robotscouter.core.data.logFailures
import com.supercilex.robotscouter.core.data.share
import com.supercilex.robotscouter.core.data.teams
import com.supercilex.robotscouter.core.data.templatesRef
import com.supercilex.robotscouter.core.data.uid
import com.supercilex.robotscouter.core.data.waitForChange
import com.supercilex.robotscouter.core.logFailures
import com.supercilex.robotscouter.core.model.Scout
import com.supercilex.robotscouter.core.model.TemplateType
import kotlinx.coroutines.experimental.async
import org.jetbrains.anko.longToast
import org.jetbrains.anko.runOnUiThread
import java.util.Date
import kotlin.math.abs

fun getTemplatesQuery(direction: Query.Direction = Query.Direction.ASCENDING): Query =
        "$FIRESTORE_OWNERS.${checkNotNull(uid)}".let {
            templatesRef.whereGreaterThanOrEqualTo(it, Date(0)).orderBy(it, direction)
        }

fun getTemplateRef(id: String) = templatesRef.document(id)

fun getTemplateMetricsRef(id: String) = getTemplateRef(id).collection(FIRESTORE_METRICS)

fun addTemplate(type: TemplateType): String {
    val ref = templatesRef.document()
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

    firestoreBatch {
        val scout = Scout(id, id)
        set(ref, scout)
        update(ref, FIRESTORE_OWNERS, mapOf(checkNotNull(uid) to scout.timestamp))
    }.logFailures(ref, id)

    async {
        val templateSnapshot = try {
            val defaultRef = defaultTemplatesRef.document(type.id.toString())
            defaultRef.get().logFailures(defaultRef).await()
        } catch (e: Exception) {
            ref.delete().logFailures(ref)
            RobotScouter.runOnUiThread { longToast(R.string.scout_add_template_not_cached_error) }
            throw e
        }

        val metrics = scoutParser.parseSnapshot(templateSnapshot).metrics.associate {
            getTemplateMetricsRef(id).document(it.ref.id) to it
        }
        firestoreBatch {
            for ((metricRef, metric) in metrics) set(metricRef, metric)
        }.logFailures(metrics.map { it.key }, metrics.map { it.value })
    }.logFailures()

    return id
}

fun ownsTemplateTask(id: String) = async {
    try {
        getTemplatesQuery().get().await()
    } catch (e: Exception) {
        CrashLogger.onFailure(e)
        emptyList<DocumentSnapshot>()
    }.map {
        scoutParser.parseSnapshot(it)
    }.any { it.id == id }
}.asTask()

suspend fun List<DocumentReference>.shareTemplates(
        block: Boolean = false
) = share(block) { token, ids ->
    QueuedDeletion.ShareToken.Template(token, ids)
}

fun Scout.getTemplateName(index: Int): String =
        name ?: RobotScouter.getString(R.string.template_tab_default_title, index + 1)

fun trashTemplate(id: String) {
    val newTemplateId = defaultTemplateId
    async {
        val teamRefs = teams.waitForChange().filter {
            id == it.templateId
        }.map { it.ref }
        firestoreBatch {
            for (ref in teamRefs) update(ref, FIRESTORE_TEMPLATE_ID, newTemplateId)
        }.logFailures(teamRefs, newTemplateId)

        if (id == newTemplateId) {
            defaultTemplateId = TemplateType.DEFAULT.id.toString()
        }

        FirebaseAppIndex.getInstance().remove(getTemplateLink(id)).logFailures()

        val ref = getTemplateRef(id)
        val snapshot = ref.get().logFailures(ref).await()
        val oppositeDate = Date(-abs(checkNotNull(snapshot.getDate(FIRESTORE_TIMESTAMP)).time))
        firestoreBatch {
            update(snapshot.reference, "$FIRESTORE_OWNERS.${checkNotNull(uid)}", oppositeDate)
            set(userDeletionQueue, QueuedDeletion.Template(id).data, SetOptions.merge())
        }.logFailures(id)
    }.logFailures()
}

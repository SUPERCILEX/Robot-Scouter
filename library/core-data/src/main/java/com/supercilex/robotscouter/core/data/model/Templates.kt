package com.supercilex.robotscouter.core.data.model

import com.google.firebase.Timestamp
import com.google.firebase.appindexing.Action
import com.google.firebase.appindexing.FirebaseAppIndex
import com.google.firebase.appindexing.FirebaseUserActions
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.supercilex.robotscouter.common.FIRESTORE_METRICS
import com.supercilex.robotscouter.common.FIRESTORE_OWNERS
import com.supercilex.robotscouter.common.FIRESTORE_TEMPLATE_ID
import com.supercilex.robotscouter.core.CrashLogger
import com.supercilex.robotscouter.core.InvocationMarker
import com.supercilex.robotscouter.core.RobotScouter
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
import com.supercilex.robotscouter.core.logBreadcrumb
import com.supercilex.robotscouter.core.model.Scout
import com.supercilex.robotscouter.core.model.TemplateType
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.asTask
import kotlinx.coroutines.tasks.await
import org.jetbrains.anko.longToast
import org.jetbrains.anko.runOnUiThread
import java.util.Date
import kotlin.math.abs

fun getTemplatesQuery(direction: Query.Direction = Query.Direction.ASCENDING): Query =
        "$FIRESTORE_OWNERS.${checkNotNull(uid)}".let {
            templatesRef.whereGreaterThanOrEqualTo(it, Timestamp(0, 0)).orderBy(it, direction)
        }

fun getTemplateRef(id: String) = templatesRef.document(id)

fun getTemplateMetricsRef(id: String) = getTemplateRef(id).collection(FIRESTORE_METRICS)

fun addTemplate(type: TemplateType): String {
    val ref = templatesRef.document()
    val id = ref.id

    logAddTemplate(id, type)
    FirebaseAppIndex.getInstance()
            .update(getTemplateIndexable(id, "Template"))
            .logFailures("addTemplate:addIndex")
    FirebaseUserActions.getInstance().end(
            Action.Builder(Action.Builder.ADD_ACTION)
                    .setObject("Template", getTemplateLink(id))
                    .setActionStatus(Action.Builder.STATUS_TYPE_COMPLETED)
                    .build()
    ).logFailures("addTemplate:addAction")

    firestoreBatch {
        val scout = Scout(id, id)
        set(ref, scout)
        update(ref, FIRESTORE_OWNERS, mapOf(checkNotNull(uid) to scout.timestamp))
    }.logFailures("addTemplate:addScout", ref, id)

    GlobalScope.launch {
        val defaultRef = defaultTemplatesRef.document(type.id.toString())
        val templateSnapshot = try {
            defaultRef.get().await()
        } catch (e: Exception) {
            ref.delete().logFailures("addTemplate:delete", ref)
            RobotScouter.runOnUiThread { longToast(R.string.scout_add_template_not_cached_error) }

            logBreadcrumb("addTemplate:getDefaultTemplate: ${defaultRef.path}")
            throw InvocationMarker(e)
        }

        val metrics = scoutParser.parseSnapshot(templateSnapshot).metrics.associateBy {
            getTemplateMetricsRef(id).document(it.ref.id)
        }
        firestoreBatch {
            for ((metricRef, metric) in metrics) set(metricRef, metric)
        }.logFailures("addTemplate:addMetric", metrics.map { it.key }, metrics.map { it.value })
    }

    return id
}

fun ownsTemplateTask(id: String) = GlobalScope.async {
    try {
        getTemplatesQuery().get().await()
    } catch (e: Exception) {
        CrashLogger.onFailure(InvocationMarker(e))
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
    GlobalScope.launch {
        val teamRefs = teams.waitForChange().filter {
            id == it.templateId
        }.map { it.ref }
        firestoreBatch {
            for (ref in teamRefs) update(ref, FIRESTORE_TEMPLATE_ID, newTemplateId)
        }.logFailures("trashTemplate", teamRefs, newTemplateId)

        if (id == newTemplateId) {
            defaultTemplateId = TemplateType.DEFAULT.id.toString()
        }
        updateTemplateTrashStatus(true, id)
    }
}

fun untrashTemplate(id: String) {
    GlobalScope.launch { updateTemplateTrashStatus(false, id) }
}

private suspend fun updateTemplateTrashStatus(delete: Boolean, id: String) {
    val ref = getTemplateRef(id)
    val snapshot = try {
        ref.get().await()
    } catch (e: Exception) {
        logBreadcrumb("updateTemplateTrashStatus: ${ref.path}")
        throw InvocationMarker(e)
    }

    val ownerField = "$FIRESTORE_OWNERS.${checkNotNull(uid)}"
    val newDate = Date(
            (if (delete) -1 else 1) * abs(checkNotNull(snapshot.getDate(ownerField)).time))
    val isTrash = newDate.time <= 0

    if (isTrash) {
        FirebaseAppIndex.getInstance()
                .remove(getTemplateLink(id))
                .logFailures("updateTemplateTrashStatus:trash")
    } else {
        FirebaseAppIndex.getInstance()
                .update(getTemplateIndexable(id, "Template"))
                .logFailures("updateTemplateTrashStatus:untrash")
    }

    firestoreBatch {
        update(ref, ownerField, newDate)
        if (isTrash) {
            set(userDeletionQueue, QueuedDeletion.Template(id).data, SetOptions.merge())
        } else {
            update(userDeletionQueue, id, FieldValue.delete())
        }
    }.logFailures("updateTemplateTrashStatus:set", id)
}

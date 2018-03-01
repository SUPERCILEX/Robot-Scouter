package com.supercilex.robotscouter.util.data.model

import com.google.firebase.appindexing.Action
import com.google.firebase.appindexing.FirebaseAppIndex
import com.google.firebase.appindexing.FirebaseUserActions
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreException.Code
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.data.model.Scout
import com.supercilex.robotscouter.data.model.TemplateType
import com.supercilex.robotscouter.util.FIRESTORE_METRICS
import com.supercilex.robotscouter.util.FIRESTORE_OWNERS
import com.supercilex.robotscouter.util.FIRESTORE_TIMESTAMP
import com.supercilex.robotscouter.util.await
import com.supercilex.robotscouter.util.data.QueuedDeletion
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
import kotlinx.coroutines.experimental.async
import org.jetbrains.anko.longToast
import org.jetbrains.anko.runOnUiThread
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

    async {
        val templateSnapshot = try {
            defaultTemplates.document(type.id.toString()).log().get().await()
        } catch (e: Exception) {
            ref.log().delete().logFailures()
            RobotScouter.runOnUiThread { longToast(R.string.scout_add_template_not_cached_error) }
            if (e is FirebaseFirestoreException && e.code == Code.UNAVAILABLE) {
                return@async
            } else {
                throw e
            }
        }

        firestoreBatch {
            scoutParser.parseSnapshot(templateSnapshot).metrics.forEach {
                set(getTemplateMetricsRef(id).document(it.ref.id).log(), it)
            }
        }
    }.logFailures()

    return id
}

fun Scout.getTemplateName(index: Int): String =
        name ?: RobotScouter.getString(R.string.template_tab_default_title, index + 1)

fun trashTemplate(id: String) {
    FirebaseAppIndex.getInstance().remove(getTemplateLink(id)).logFailures()
    async {
        val snapshot = getTemplateRef(id).log().get().await()
        val oppositeDate = Date(-abs(snapshot.getDate(FIRESTORE_TIMESTAMP).time))
        firestoreBatch {
            update(snapshot.reference.log(), "$FIRESTORE_OWNERS.${uid!!}", oppositeDate)
            set(userDeletionQueue.log(), QueuedDeletion.Template(id).data, SetOptions.merge())
        }.await()
    }.logFailures()
}

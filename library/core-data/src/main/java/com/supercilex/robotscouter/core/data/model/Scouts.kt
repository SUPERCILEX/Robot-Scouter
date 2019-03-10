package com.supercilex.robotscouter.core.data.model

import com.firebase.ui.firestore.ObservableSnapshotArray
import com.firebase.ui.firestore.SnapshotParser
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.supercilex.robotscouter.common.FIRESTORE_METRICS
import com.supercilex.robotscouter.common.FIRESTORE_NAME
import com.supercilex.robotscouter.common.FIRESTORE_SCOUTS
import com.supercilex.robotscouter.common.FIRESTORE_TEMPLATE_ID
import com.supercilex.robotscouter.common.FIRESTORE_TIMESTAMP
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.await
import com.supercilex.robotscouter.core.data.QueuedDeletion
import com.supercilex.robotscouter.core.data.R
import com.supercilex.robotscouter.core.data.defaultTemplatesRef
import com.supercilex.robotscouter.core.data.firestoreBatch
import com.supercilex.robotscouter.core.data.logAddScout
import com.supercilex.robotscouter.core.data.logFailures
import com.supercilex.robotscouter.core.data.waitForChange
import com.supercilex.robotscouter.core.logFailures
import com.supercilex.robotscouter.core.model.Scout
import com.supercilex.robotscouter.core.model.Team
import com.supercilex.robotscouter.core.model.TemplateType
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.jetbrains.anko.longToast
import org.jetbrains.anko.runOnUiThread
import java.util.Date
import kotlin.math.abs

val scoutParser = SnapshotParser { snapshot ->
    Scout(snapshot.id,
          checkNotNull(snapshot.getString(FIRESTORE_TEMPLATE_ID)),
          snapshot.getString(FIRESTORE_NAME),
          checkNotNull(snapshot.getTimestamp(FIRESTORE_TIMESTAMP)),
          @Suppress("UNCHECKED_CAST") // Our data is stored as a map of metrics
          (snapshot[FIRESTORE_METRICS] as Map<String, Any?>? ?: emptyMap()).map {
              parseMetric(it.value as Map<String, Any?>, FirebaseFirestore.getInstance().document(
                      "${snapshot.reference.path}/$FIRESTORE_METRICS/${it.key}"))
          })
}

val Team.scoutsRef get() = ref.collection(FIRESTORE_SCOUTS)

fun Team.getScoutsQuery(direction: Query.Direction = Query.Direction.ASCENDING): Query =
        FIRESTORE_TIMESTAMP.let {
            scoutsRef.whereGreaterThanOrEqualTo(it, Timestamp(0, 0)).orderBy(it, direction)
        }

fun Team.getScoutMetricsRef(id: String) = scoutsRef.document(id).collection(FIRESTORE_METRICS)

fun Team.addScout(overrideId: String?, existingScouts: ObservableSnapshotArray<Scout>): String {
    val templateId = overrideId ?: templateId
    val scoutRef = scoutsRef.document()

    logAddScout(id, templateId)
    Scout(scoutRef.id, templateId).let { scoutRef.set(it).logFailures(scoutRef, it) }

    GlobalScope.async {
        val templateName = try {
            val metricsRef = getScoutMetricsRef(scoutRef.id)
            TemplateType.coerce(templateId)?.let { type ->
                val scout = scoutParser.parseSnapshot(
                        defaultTemplatesRef.document(type.id.toString()).get().await())

                val metrics = scout.metrics.associate {
                    metricsRef.document(it.ref.id) to it
                }
                firestoreBatch {
                    for ((metricRef, metric) in metrics) set(metricRef, metric)
                }.logFailures(metrics.map { it.key }, metrics.map { it.value })

                scout.name
            } ?: run {
                val deferredName = async {
                    scoutParser.parseSnapshot(
                            getTemplateRef(templateId).get().logFailures(templateId).await()
                    ).name
                }

                val snapshot = getTemplateRef(templateId)
                        .collection(FIRESTORE_METRICS).get().logFailures(templateId).await()
                val metrics = snapshot.documents.associate { it.id to checkNotNull(it.data) }
                firestoreBatch {
                    for ((id, data) in metrics) {
                        set(metricsRef.document(id), data)
                    }
                }.logFailures(scoutRef, metrics)

                try {
                    deferredName.await()
                } catch (e: FirebaseFirestoreException) {
                    null // Don't abort scout addition if name fetch failed
                }
            }
        } catch (e: Exception) {
            scoutRef.delete().logFailures(scoutRef)
            RobotScouter.runOnUiThread { longToast(R.string.scout_add_template_not_cached_error) }
            throw e
        } ?: return@async

        val nExistingTemplates = existingScouts.waitForChange().map {
            it.templateId
        }.groupBy { it }[templateId]?.size ?: return@async

        scoutRef.update(FIRESTORE_NAME, "$templateName $nExistingTemplates").logFailures(scoutRef)
    }.logFailures()

    return scoutRef.id
}

fun Team.trashScout(id: String) = updateScoutDate(id) { -abs(it) }

fun Team.untrashScout(id: String) = updateScoutDate(id) { abs(it) }

private fun Team.updateScoutDate(id: String, update: (Long) -> Long) {
    GlobalScope.async {
        val ref = scoutsRef.document(id)
        val snapshot = ref.get().logFailures(ref).await()
        if (!snapshot.exists()) return@async

        val oppositeDate = Date(update(checkNotNull(snapshot.getDate(FIRESTORE_TIMESTAMP)).time))
        firestoreBatch {
            this.update(ref, FIRESTORE_TIMESTAMP, oppositeDate)
            if (oppositeDate.time > 0) {
                this.update(userDeletionQueue, id, FieldValue.delete())
            } else {
                set(userDeletionQueue,
                    QueuedDeletion.Scout(id, this@updateScoutDate.id).data,
                    SetOptions.merge())
            }
        }.logFailures(ref, this@updateScoutDate)
    }.logFailures()
}

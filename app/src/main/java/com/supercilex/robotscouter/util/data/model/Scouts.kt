package com.supercilex.robotscouter.util.data.model

import android.arch.lifecycle.LiveData
import com.firebase.ui.firestore.ObservableSnapshotArray
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.data.model.Scout
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.data.model.TemplateType
import com.supercilex.robotscouter.util.FIRESTORE_METRICS
import com.supercilex.robotscouter.util.FIRESTORE_NAME
import com.supercilex.robotscouter.util.FIRESTORE_SCOUTS
import com.supercilex.robotscouter.util.FIRESTORE_TIMESTAMP
import com.supercilex.robotscouter.util.await
import com.supercilex.robotscouter.util.data.QueuedDeletion
import com.supercilex.robotscouter.util.data.firestoreBatch
import com.supercilex.robotscouter.util.data.isOffline
import com.supercilex.robotscouter.util.data.observeOnDataChanged
import com.supercilex.robotscouter.util.data.observeOnce
import com.supercilex.robotscouter.util.data.scoutParser
import com.supercilex.robotscouter.util.defaultTemplates
import com.supercilex.robotscouter.util.logAddScout
import com.supercilex.robotscouter.util.logFailures
import kotlinx.coroutines.experimental.async
import org.jetbrains.anko.longToast
import org.jetbrains.anko.runOnUiThread
import java.util.Date
import kotlin.math.abs

fun Team.getScoutsRef() = ref.collection(FIRESTORE_SCOUTS)

fun Team.getScoutsQuery(direction: Query.Direction = Query.Direction.ASCENDING): Query =
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

    logAddScout(id, templateId)
    Scout(scoutRef.id, templateId).let { scoutRef.set(it).logFailures(scoutRef, it) }

    async {
        val templateName = try {
            val metricsRef = getScoutMetricsRef(scoutRef.id)
            TemplateType.coerce(templateId)?.let { type ->
                val scout = scoutParser.parseSnapshot(
                        defaultTemplates.document(type.id.toString()).get().await())

                val metrics = scout.metrics.associate {
                    metricsRef.document(it.ref.id) to it
                }
                firestoreBatch {
                    for ((metricRef, metric) in metrics) set(metricRef, metric)
                }.logFailures(metrics.map { it.key }, metrics.map { it.value })

                scout.name
            } ?: run {
                val deferredName = async {
                    val template = getTemplateRef(templateId).get().logFailures(templateId) {
                        (it as? FirebaseFirestoreException)?.isOffline == true
                    }.await()
                    scoutParser.parseSnapshot(template).name
                }

                async {
                    val snapshot = getTemplateRef(templateId)
                            .collection(FIRESTORE_METRICS).get().logFailures(templateId).await()
                    val metrics = snapshot.documents.associate { it.id to it.data }
                    firestoreBatch {
                        for ((id, data) in metrics) {
                            set(metricsRef.document(id), data)
                        }
                    }.logFailures(scoutRef, metrics) {
                        (it as? FirebaseFirestoreException)?.isOffline == true
                    }
                }.await()

                try {
                    deferredName.await()
                } catch (e: FirebaseFirestoreException) {
                    null // Don't abort scout addition if name fetch failed
                }
            }
        } catch (e: Exception) {
            scoutRef.delete().logFailures(scoutRef)
            RobotScouter.runOnUiThread { longToast(R.string.scout_add_template_not_cached_error) }
            if (e is FirebaseFirestoreException && e.isOffline) {
                null
            } else {
                throw e
            }
        } ?: return@async

        val nExistingTemplates = existingScouts.observeOnDataChanged().observeOnce()?.map {
            it.templateId
        }?.groupBy { it }?.get(templateId)?.size ?: return@async

        scoutRef.update(FIRESTORE_NAME, "$templateName $nExistingTemplates").logFailures(scoutRef)
    }.logFailures()

    return scoutRef.id
}

fun Team.trashScout(id: String) = updateScoutDate(id) { -abs(it) }

fun Team.untrashScout(id: String) = updateScoutDate(id) { abs(it) }

private fun Team.updateScoutDate(id: String, update: (Long) -> Long) {
    async {
        val ref = getScoutsRef().document(id)
        val snapshot = ref.get().logFailures(ref).await()
        if (!snapshot.exists()) return@async

        val oppositeDate = Date(update(snapshot.getDate(FIRESTORE_TIMESTAMP).time))
        firestoreBatch {
            this.update(ref, FIRESTORE_TIMESTAMP, oppositeDate)
            if (oppositeDate.time > 0) {
                this.update(userDeletionQueue, id, FieldValue.delete())
            } else {
                set(userDeletionQueue,
                    QueuedDeletion.Scout(id, this@updateScoutDate.id).data,
                    SetOptions.merge())
            }
        }.logFailures(ref, this@updateScoutDate).await()
    }.logFailures()
}

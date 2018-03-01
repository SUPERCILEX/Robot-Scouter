package com.supercilex.robotscouter.util.data.model

import android.arch.lifecycle.LiveData
import com.firebase.ui.firestore.ObservableSnapshotArray
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreException.Code
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.data.model.Scout
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.data.model.TemplateType
import com.supercilex.robotscouter.util.CrashLogger
import com.supercilex.robotscouter.util.FIRESTORE_METRICS
import com.supercilex.robotscouter.util.FIRESTORE_NAME
import com.supercilex.robotscouter.util.FIRESTORE_SCOUTS
import com.supercilex.robotscouter.util.FIRESTORE_TIMESTAMP
import com.supercilex.robotscouter.util.await
import com.supercilex.robotscouter.util.data.QueuedDeletion
import com.supercilex.robotscouter.util.data.firestoreBatch
import com.supercilex.robotscouter.util.data.observeOnDataChanged
import com.supercilex.robotscouter.util.data.observeOnce
import com.supercilex.robotscouter.util.data.scoutParser
import com.supercilex.robotscouter.util.defaultTemplates
import com.supercilex.robotscouter.util.log
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
    scoutRef.log().set(Scout(scoutRef.id, templateId)).logFailures()

    async {
        val templateName = try {
            TemplateType.coerce(templateId)?.let { type ->
                val scout = scoutParser.parseSnapshot(
                        defaultTemplates.document(type.id.toString()).log().get().await())

                firestoreBatch {
                    scout.metrics.forEach {
                        set(getScoutMetricsRef(scoutRef.id).document(it.ref.id).log(), it)
                    }
                }.logFailures()

                scout.name
            } ?: run {
                val deferredName = async {
                    scoutParser.parseSnapshot(getTemplateRef(templateId).log().get().await()).name
                }

                async {
                    val snapshot = getTemplateRef(templateId)
                            .collection(FIRESTORE_METRICS).log().get().await()
                    val metrics = snapshot.documents.associate { it.id to it.data }
                    firestoreBatch {
                        for ((id, data) in metrics) {
                            set(getScoutMetricsRef(scoutRef.id).document(id).log(), data)
                        }
                    }
                }.await()

                try {
                    deferredName.await()
                } catch (e: Exception) {
                    if (e !is FirebaseFirestoreException || e.code != Code.UNAVAILABLE) {
                        CrashLogger.onFailure(e)
                    }
                    null // Don't abort scout addition if name fetch failed
                }
            }
        } catch (e: Exception) {
            scoutRef.log().delete().logFailures()
            RobotScouter.runOnUiThread { longToast(R.string.scout_add_template_not_cached_error) }
            if (e is FirebaseFirestoreException && e.code == Code.UNAVAILABLE) {
                null
            } else {
                throw e
            }
        } ?: return@async

        val nExistingTemplates = existingScouts.observeOnDataChanged().observeOnce()?.map {
            it.templateId
        }?.groupBy { it }?.get(templateId)?.size ?: return@async

        scoutRef.log().update(FIRESTORE_NAME, "$templateName $nExistingTemplates").logFailures()
    }.logFailures()

    return scoutRef.id
}

fun Team.trashScout(id: String) = updateScoutDate(id) { -abs(it) }

fun Team.untrashScout(id: String) = updateScoutDate(id) { abs(it) }

private fun Team.updateScoutDate(id: String, update: (Long) -> Long) {
    async {
        val snapshot = getScoutsRef().document(id).log().get().await()
        if (!snapshot.exists()) return@async
        val oppositeDate = Date(update(snapshot.getDate(FIRESTORE_TIMESTAMP).time))
        firestoreBatch {
            this.update(snapshot.reference.log(), FIRESTORE_TIMESTAMP, oppositeDate)
            if (oppositeDate.time > 0) {
                this.update(userDeletionQueue.log(), id, FieldValue.delete())
            } else {
                set(userDeletionQueue.log(),
                    QueuedDeletion.Scout(id, this@updateScoutDate.id).data,
                    SetOptions.merge())
            }
        }.await()
    }.logFailures()
}

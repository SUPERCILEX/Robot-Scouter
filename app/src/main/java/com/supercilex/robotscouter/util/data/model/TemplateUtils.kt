package com.supercilex.robotscouter.util.data.model

import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.Query
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.data.model.Scout
import com.supercilex.robotscouter.data.model.TemplateType
import com.supercilex.robotscouter.util.AsyncTaskExecutor
import com.supercilex.robotscouter.util.FIRESTORE_DEFAULT_TEMPLATES
import com.supercilex.robotscouter.util.FIRESTORE_METRICS
import com.supercilex.robotscouter.util.FIRESTORE_OWNERS
import com.supercilex.robotscouter.util.FIRESTORE_TEMPLATES
import com.supercilex.robotscouter.util.data.SCOUT_PARSER
import com.supercilex.robotscouter.util.data.batch
import com.supercilex.robotscouter.util.data.delete
import com.supercilex.robotscouter.util.data.firestoreBatch
import com.supercilex.robotscouter.util.logAddTemplateEvent
import com.supercilex.robotscouter.util.uid
import java.util.Date

fun getTemplatesQuery(direction: Query.Direction = Query.Direction.ASCENDING): Query =
        "$FIRESTORE_OWNERS.${uid!!}".let {
            FIRESTORE_TEMPLATES.whereGreaterThanOrEqualTo(it, Date(0)).orderBy(it, direction)
        }

fun getTemplateRef(id: String) = FIRESTORE_TEMPLATES.document(id)

fun getTemplateMetricsRef(id: String) = getTemplateRef(id).collection(FIRESTORE_METRICS)

fun addTemplate(@TemplateType type: Int): String {
    val ref = FIRESTORE_TEMPLATES.document()
    val id = ref.id

    ref.batch {
        val scout = Scout(id, id)
        set(it, scout)
        update(it, FIRESTORE_OWNERS, mapOf(uid!! to scout.timestamp))
    }

    FIRESTORE_DEFAULT_TEMPLATES.get().addOnSuccessListener(
            AsyncTaskExecutor, OnSuccessListener {
        firestoreBatch {
            it.map {
                SCOUT_PARSER.parseSnapshot(it)
            }.find { it.id == type.toString() }!!.metrics.forEach {
                set(getTemplateMetricsRef(id).document(it.ref.id), it)
            }
        }
    })

    logAddTemplateEvent(id)
    return id
}

fun Scout.getTemplateName(index: Int): String =
        name ?: RobotScouter.INSTANCE.getString(R.string.title_template_tab, index + 1)

fun deleteTemplate(id: String) {
    getTemplateMetricsRef(id).delete().addOnSuccessListener {
        getTemplateRef(id).delete()
    }
}

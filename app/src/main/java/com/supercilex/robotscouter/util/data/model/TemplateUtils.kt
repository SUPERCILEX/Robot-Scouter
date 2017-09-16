package com.supercilex.robotscouter.util.data.model

import com.google.android.gms.tasks.Tasks
import com.google.firebase.appindexing.Indexable
import com.google.firebase.appindexing.builders.Indexables
import com.google.firebase.firestore.Query
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.data.model.MATCH
import com.supercilex.robotscouter.data.model.PIT
import com.supercilex.robotscouter.data.model.Scout
import com.supercilex.robotscouter.data.model.TemplateType
import com.supercilex.robotscouter.util.FIRESTORE_METRICS
import com.supercilex.robotscouter.util.FIRESTORE_OWNERS
import com.supercilex.robotscouter.util.FIRESTORE_TEMPLATES
import com.supercilex.robotscouter.util.ID_QUERY
import com.supercilex.robotscouter.util.TEMPLATES_LINK_BASE
import com.supercilex.robotscouter.util.data.DefaultTemplatesLiveData
import com.supercilex.robotscouter.util.data.batch
import com.supercilex.robotscouter.util.data.delete
import com.supercilex.robotscouter.util.data.observeOnDataChanged
import com.supercilex.robotscouter.util.data.observeOnce
import com.supercilex.robotscouter.util.logAddTemplateEvent
import com.supercilex.robotscouter.util.uid

fun getTemplatesQuery(direction: Query.Direction = Query.Direction.ASCENDING): Query =
        "$FIRESTORE_OWNERS.${uid!!}".let {
            FIRESTORE_TEMPLATES.whereGreaterThanOrEqualTo(it, 0).orderBy(it, direction)
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

    when (type) {
        MATCH, PIT -> DefaultTemplatesLiveData.observeOnDataChanged().observeOnce {
            it.find { it.id == type.toString() }!!.metrics.forEach {
                getTemplateMetricsRef(id).document(it.ref.id).set(it)
            }
            Tasks.forResult(null)
        }
    }

    logAddTemplateEvent(id)
    return id
}

fun Scout.getTemplateName(index: Int): String =
        name ?: RobotScouter.INSTANCE.getString(R.string.title_template_tab, index + 1)

fun getTemplateLink(templateId: String) = "$TEMPLATES_LINK_BASE?$ID_QUERY=$templateId"

fun getTemplateIndexable(templateId: String, templateName: String): Indexable =
        Indexables.digitalDocumentBuilder()
                .setUrl(getTemplateLink(templateId))
                .setName(templateName)
                .setMetadata(Indexable.Metadata.Builder().setWorksOffline(true))
                .build()

fun deleteTemplate(id: String) {
    getTemplateRef(id).delete()
    getTemplateMetricsRef(id).delete()
}

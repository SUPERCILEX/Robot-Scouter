package com.supercilex.robotscouter.util.data.model

import com.google.android.gms.tasks.Tasks
import com.google.firebase.appindexing.Indexable
import com.google.firebase.appindexing.builders.Indexables
import com.google.firebase.database.DatabaseReference
import com.supercilex.robotscouter.data.model.EMPTY
import com.supercilex.robotscouter.data.model.MATCH
import com.supercilex.robotscouter.data.model.PIT
import com.supercilex.robotscouter.data.model.TemplateType
import com.supercilex.robotscouter.util.FIREBASE_METRICS
import com.supercilex.robotscouter.util.FIREBASE_TEMPLATES
import com.supercilex.robotscouter.util.FIREBASE_TEMPLATE_INDICES
import com.supercilex.robotscouter.util.KEY_QUERY
import com.supercilex.robotscouter.util.TEMPLATES_LINK_BASE
import com.supercilex.robotscouter.util.data.DefaultTemplatesLiveData
import com.supercilex.robotscouter.util.data.copySnapshots
import com.supercilex.robotscouter.util.data.observeOnDataChanged
import com.supercilex.robotscouter.util.data.observeOnce
import com.supercilex.robotscouter.util.logAddTemplateEvent
import com.supercilex.robotscouter.util.uid

val templateIndicesRef: DatabaseReference get() = getTemplateIndicesRef(uid!!)

fun getTemplateIndicesRef(uid: String): DatabaseReference = FIREBASE_TEMPLATE_INDICES.child(uid)

fun getTemplateMetricsRef(key: String): DatabaseReference =
        FIREBASE_TEMPLATES.child(key).child(FIREBASE_METRICS)

fun addTemplate(@TemplateType type: Int): String {
    val indicesRef = templateIndicesRef.push()
    val key = indicesRef.key

    when (type) {
        EMPTY -> indicesRef.setValue(true)
        MATCH, PIT -> DefaultTemplatesLiveData.observeOnDataChanged().observeOnce {
            copySnapshots(it[type], getTemplateMetricsRef(key))
            indicesRef.setValue(true)

            Tasks.forResult(null)
        }
        else -> throw IllegalStateException("Unknown template type: $type")
    }

    logAddTemplateEvent(key)
    return key
}

fun getTemplateLink(templateKey: String) = "$TEMPLATES_LINK_BASE?$KEY_QUERY=$templateKey"

fun getTemplateIndexable(templateKey: String, templateName: String): Indexable =
        Indexables.digitalDocumentBuilder()
                .setUrl(getTemplateLink(templateKey))
                .setName(templateName)
                .setMetadata(Indexable.Metadata.Builder().setWorksOffline(true))
                .build()

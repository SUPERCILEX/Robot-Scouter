package com.supercilex.robotscouter.util.data

import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.appindexing.Action
import com.google.firebase.appindexing.Indexable
import com.google.firebase.appindexing.Scope
import com.google.firebase.appindexing.builders.Actions
import com.google.firebase.appindexing.builders.Indexables
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.APP_LINK_BASE
import com.supercilex.robotscouter.util.FIRESTORE_ACTIVE_TOKENS
import com.supercilex.robotscouter.util.FIRESTORE_OWNERS
import com.supercilex.robotscouter.util.FIRESTORE_PENDING_APPROVALS
import com.supercilex.robotscouter.util.logFailures
import com.supercilex.robotscouter.util.teams
import com.supercilex.robotscouter.util.templates
import com.supercilex.robotscouter.util.uid

const val ACTION_FROM_DEEP_LINK = "com.supercilex.robotscouter.action.FROM_DEEP_LINK"
const val KEYS = "keys"

private val TEAMS_LINK_BASE = "$APP_LINK_BASE${teams.id}/"
private val TEMPLATES_LINK_BASE = "$APP_LINK_BASE${templates.id}/"

val generateToken: String get() = FirebaseFirestore.getInstance().collection("null").document().id

val Team.deepLink: String get() = listOf(this).getTeamsLink()
val Team.viewAction: Action get() = Actions.newView(toString(), deepLink)
val Team.indexable: Indexable
    get() = Indexables.digitalDocumentBuilder()
            .setUrl(deepLink)
            .setName(toString())
            .apply { setImage(media ?: return@apply) }
            .setMetadata(Indexable.Metadata.Builder()
                                 .setWorksOffline(true)
                                 .setScope(Scope.CROSS_DEVICE))
            .build()

fun getTemplateIndexable(templateId: String, templateName: String): Indexable =
        Indexables.digitalDocumentBuilder()
                .setUrl(getTemplateLink(templateId))
                .setName(templateName)
                .setMetadata(Indexable.Metadata.Builder()
                                     .setWorksOffline(true)
                                     .setScope(Scope.CROSS_DEVICE))
                .build()

fun List<Team>.getTeamsLink(token: String? = null): String =
        generateUrl(TEAMS_LINK_BASE, token) { id to number.toString() }

fun getTemplateLink(templateId: String, token: String? = null): String =
        listOf(templateId).generateUrl(TEMPLATES_LINK_BASE, token) { this to true.toString() }

inline fun updateOwner(
        refs: Iterable<DocumentReference>,
        token: String,
        prevUid: String?,
        newValue: (DocumentReference) -> Any
): Task<Void> {
    val pendingApprovalPath = FieldPath.of(FIRESTORE_PENDING_APPROVALS, uid!!)
    val oldOwnerPath = prevUid?.let { FieldPath.of(FIRESTORE_OWNERS, it) }
    val newOwnerPath = FieldPath.of(FIRESTORE_OWNERS, uid!!)

    return Tasks.whenAll(refs.map { ref ->
        firestoreBatch {
            update(ref, pendingApprovalPath, token)
            oldOwnerPath?.let { update(ref, it, FieldValue.delete()) }
            update(ref, newOwnerPath, newValue(ref))
        }.addOnSuccessListener {
            ref.update(pendingApprovalPath, FieldValue.delete())
        }.logFailures()
    })
}

private inline fun <T> List<T>.generateUrl(
        linkBase: String,
        token: String?,
        queryParamsGenerator: T.() -> Pair<String, String>
): String {
    val builder = Uri.Builder().path(linkBase).encodeToken(token)

    val keys = ArrayList<String>(size)
    for (item in this) {
        val (key, value) = item.queryParamsGenerator()
        builder.appendQueryParameter(key, value)
        keys += key
    }
    builder.appendQueryParameter(KEYS, keys.joinToString(","))

    return Uri.decode(builder.build().toString())
}

private fun Uri.Builder.encodeToken(token: String?): Uri.Builder =
        token?.let { appendQueryParameter(FIRESTORE_ACTIVE_TOKENS, it) } ?: this

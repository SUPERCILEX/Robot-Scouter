package com.supercilex.robotscouter.util.data

import android.net.Uri
import android.support.annotation.WorkerThread
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
import com.supercilex.robotscouter.util.await
import com.supercilex.robotscouter.util.logFailures
import com.supercilex.robotscouter.util.teamsRef
import com.supercilex.robotscouter.util.templatesRef
import com.supercilex.robotscouter.util.uid
import kotlinx.coroutines.experimental.async
import java.io.File

const val ACTION_FROM_DEEP_LINK = "com.supercilex.robotscouter.action.FROM_DEEP_LINK"
const val KEYS = "keys"

private val TEAMS_LINK_BASE = "$APP_LINK_BASE${teamsRef.id}/"
private val TEMPLATES_LINK_BASE = "$APP_LINK_BASE${templatesRef.id}/"

val generateToken: String get() = FirebaseFirestore.getInstance().collection("null").document().id

val Team.deepLink: String get() = listOf(this).getTeamsLink()

val Team.viewAction: Action get() = Actions.newView(toString(), deepLink)

@get:WorkerThread
val Team.indexable: Indexable
    get() = Indexables.digitalDocumentBuilder()
            .setUrl(deepLink)
            .setName(toString())
            .apply {
                val media = media
                if (media?.isNotBlank() == true && !File(media).exists()) {
                    setImage(media)
                }
            }
            .setMetadata(Indexable.Metadata.Builder()
                                 .setWorksOffline(true)
                                 .setScope(Scope.CROSS_DEVICE))
            .build()

fun List<Team>.getTeamsLink(token: String? = null): String =
        generateUrl(TEAMS_LINK_BASE, token) { id to number.toString() }

fun getTemplateLink(templateId: String, token: String? = null): String =
        listOf(templateId).generateUrl(TEMPLATES_LINK_BASE, token) { this to true.toString() }

fun getTemplateViewAction(id: String, name: String): Action =
        Actions.newView(name, getTemplateLink(id))

fun getTemplateIndexable(templateId: String, templateName: String): Indexable =
        Indexables.digitalDocumentBuilder()
                .setUrl(getTemplateLink(templateId))
                .setName(templateName)
                .setMetadata(Indexable.Metadata.Builder()
                                     .setWorksOffline(true)
                                     .setScope(Scope.CROSS_DEVICE))
                .build()

suspend fun updateOwner(
        refs: Iterable<DocumentReference>,
        token: String,
        prevUid: String?,
        newValue: (DocumentReference) -> Any
) {
    val pendingApprovalPath = FieldPath.of(FIRESTORE_PENDING_APPROVALS, uid!!)
    val oldOwnerPath = prevUid?.let { FieldPath.of(FIRESTORE_OWNERS, it) }
    val newOwnerPath = FieldPath.of(FIRESTORE_OWNERS, uid!!)

    refs.map { ref ->
        async {
            ref.batch {
                update(it, pendingApprovalPath, token)
                oldOwnerPath?.let { update(ref, it, FieldValue.delete()) }
                update(it, newOwnerPath, newValue(it))
            }.logFailures(ref, "Token: $token, from user: $prevUid").await()
            ref.update(pendingApprovalPath, FieldValue.delete()).logFailures(ref)
        }
    }.await()
}

private inline fun <T> List<T>.generateUrl(
        linkBase: String,
        token: String?,
        crossinline queryParamsGenerator: T.() -> Pair<String, String>
): String {
    val builder = Uri.Builder().path(linkBase).encodeToken(token)

    builder.appendQueryParameter(KEYS, joinToString(",") {
        val (key, value) = it.queryParamsGenerator()
        builder.appendQueryParameter(key, value)
        key
    })

    return Uri.decode(builder.build().toString())
}

private fun Uri.Builder.encodeToken(token: String?): Uri.Builder =
        token?.let { appendQueryParameter(FIRESTORE_ACTIVE_TOKENS, it) } ?: this

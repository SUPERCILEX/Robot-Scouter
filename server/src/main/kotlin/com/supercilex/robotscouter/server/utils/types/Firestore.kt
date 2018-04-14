@file:Suppress(
        "INTERFACE_WITH_SUPERCLASS",
        "OVERRIDING_FINAL_MEMBER",
        "RETURN_TYPE_MISMATCH_ON_OVERRIDE",
        "CONFLICTING_OVERLOADS",
        "unused"
)
@file:[JsQualifier("FirebaseFirestore") JsModule("@google-cloud/firestore")]

package com.supercilex.robotscouter.server.utils.types

import kotlin.js.Json
import kotlin.js.Promise

external class Firestore(options: Any? = definedExternally) {
    fun collection(collectionPath: String): CollectionReference = definedExternally
    fun doc(documentPath: String): DocumentReference = definedExternally
    fun getAll(vararg documentRef: DocumentReference): Promise<Array<DocumentSnapshot>> = definedExternally
    fun <T> runTransaction(updateFunction: (transaction: Transaction) -> Promise<T>): Promise<T> = definedExternally
    fun batch(): WriteBatch = definedExternally
}

external class GeoPoint(latitude: Number, longitude: Number) {
    val latitude: Number = definedExternally
    val longitude: Number = definedExternally
}

external class Transaction {
    fun get(documentRef: DocumentReference): Promise<DocumentSnapshot> = definedExternally
    fun get(query: Query): Promise<QuerySnapshot> = definedExternally
    fun create(documentRef: DocumentReference, data: Json): Transaction = definedExternally
    fun set(documentRef: DocumentReference, data: Json, options: SetOptions? = definedExternally): Transaction = definedExternally
    fun update(documentRef: DocumentReference, data: Json, precondition: Precondition? = definedExternally): Transaction = definedExternally
    fun update(documentRef: DocumentReference, field: String, value: Any, vararg fieldsOrPrecondition: Any): Transaction = definedExternally
    fun delete(documentRef: DocumentReference, precondition: Precondition? = definedExternally): Transaction = definedExternally
}

external class WriteBatch {
    fun create(documentRef: DocumentReference, data: Json): WriteBatch = definedExternally
    fun set(documentRef: DocumentReference, data: Json, options: SetOptions? = definedExternally): WriteBatch = definedExternally
    fun update(documentRef: DocumentReference, data: Json, precondition: Precondition? = definedExternally): WriteBatch = definedExternally
    fun update(documentRef: DocumentReference, field: String, value: Any, vararg fieldsOrPrecondition: Any): WriteBatch = definedExternally
    fun delete(documentRef: DocumentReference, precondition: Precondition? = definedExternally): WriteBatch = definedExternally
    fun commit(): Promise<Array<WriteResult>> = definedExternally
}

external interface Precondition {
    val lastUpdateTime: String? get() = definedExternally
}

external interface SetOptions {
    val merge: Boolean? get() = definedExternally
}

external class WriteResult {
    val writeTime: String = definedExternally
}

external class DocumentReference {
    val id: String = definedExternally
    val firestore: Firestore = definedExternally
    val parent: CollectionReference = definedExternally
    val path: String = definedExternally
    fun collection(collectionPath: String): CollectionReference = definedExternally
    fun create(data: Json): Promise<WriteResult> = definedExternally
    fun set(data: Json, options: SetOptions? = definedExternally): Promise<WriteResult> = definedExternally
    fun update(data: Json, precondition: Precondition? = definedExternally): Promise<WriteResult> = definedExternally
    fun update(field: String, value: Any, vararg moreFieldsOrPrecondition: Any): Promise<WriteResult> = definedExternally
    fun delete(precondition: Precondition? = definedExternally): Promise<WriteResult> = definedExternally
    fun get(): Promise<DocumentSnapshot> = definedExternally
    fun onSnapshot(onNext: (snapshot: DocumentSnapshot) -> Unit, onError: ((error: Error) -> Unit)? = definedExternally): () -> Unit = definedExternally
}

external class DocumentSnapshot {
    val exists: Boolean = definedExternally
    val ref: DocumentReference = definedExternally
    val id: String = definedExternally
    val createTime: String = definedExternally
    val updateTime: String = definedExternally
    val readTime: String = definedExternally
    fun data(): Json = definedExternally
    fun get(fieldPath: String): Any = definedExternally
}

open external class Query {
    val firestore: Firestore = definedExternally
    fun where(fieldPath: String, opStr: String, value: Any?): Query = definedExternally
    fun orderBy(fieldPath: String, directionStr: Any? = definedExternally): Query = definedExternally
    fun limit(limit: Number): Query = definedExternally
    fun offset(offset: Number): Query = definedExternally
    fun select(vararg field: dynamic): Query = definedExternally
    fun startAt(vararg fieldValues: Any): Query = definedExternally
    fun startAfter(vararg fieldValues: Any): Query = definedExternally
    fun endBefore(vararg fieldValues: Any): Query = definedExternally
    fun endAt(vararg fieldValues: Any): Query = definedExternally
    fun get(): Promise<QuerySnapshot> = definedExternally
    fun onSnapshot(onNext: (snapshot: QuerySnapshot) -> Unit, onError: ((error: Error) -> Unit)? = definedExternally): () -> Unit = definedExternally
}

external class QuerySnapshot {
    val query: Query = definedExternally
    val docChanges: Array<DocumentChange> = definedExternally
    val docs: Array<DocumentSnapshot> = definedExternally
    val size: Number = definedExternally
    val empty: Boolean = definedExternally
    val readTime: String = definedExternally
    fun forEach(callback: (result: DocumentSnapshot) -> Unit, thisArg: Any? = definedExternally): Unit = definedExternally
}

external interface DocumentChange {
    val type: dynamic
    val doc: DocumentSnapshot
    val oldIndex: Number
    val newIndex: Number
}

external class CollectionReference : Query {
    val id: String = definedExternally
    val parent: DocumentReference? = definedExternally
    val path: String = definedExternally
    fun doc(documentPath: String? = definedExternally): DocumentReference = definedExternally
    fun add(data: Json): Promise<DocumentReference> = definedExternally
}

external class DatabaseBuilder {
    fun namespace(namespace: String): NamespaceBuilder = definedExternally
    fun document(path: String): DocumentBuilder = definedExternally
}

external class NamespaceBuilder {
    fun document(path: String): DocumentBuilder = definedExternally
}

external interface DeltaDocumentSnapshot {
    val exists: Boolean
    val ref: DocumentReference
    val id: String
    val createTime: String
    val updateTime: String
    val readTime: String
    val previous: DeltaDocumentSnapshot
    val get: (key: String) -> Any?
    fun data(): Json
}

external class DocumentBuilder {
    fun onWrite(handler: (event: Change<DeltaDocumentSnapshot>, context: EventContext) -> Promise<*>?): dynamic = definedExternally
    fun onCreate(handler: (event: Change<DeltaDocumentSnapshot>, context: EventContext) -> Promise<*>?): dynamic = definedExternally
    fun onUpdate(handler: (event: Change<DeltaDocumentSnapshot>, context: EventContext) -> Promise<*>?): dynamic = definedExternally
    fun onDelete(handler: (event: Change<DeltaDocumentSnapshot>, context: EventContext) -> Promise<*>?): dynamic = definedExternally
    fun onOperation(handler: Any, eventType: Any): Unit = definedExternally
}

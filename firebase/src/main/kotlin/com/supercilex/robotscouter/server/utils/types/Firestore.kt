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
    var lastUpdateTime: String? get() = definedExternally; set(value) = definedExternally
}

external interface SetOptions {
    var merge: Boolean? get() = definedExternally; set(value) = definedExternally
}

external class WriteResult {
    var writeTime: String = definedExternally
}

external class DocumentReference {
    var id: String = definedExternally
    var firestore: Firestore = definedExternally
    var parent: CollectionReference = definedExternally
    var path: String = definedExternally
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
    var exists: Boolean = definedExternally
    var ref: DocumentReference = definedExternally
    var id: String = definedExternally
    var createTime: String = definedExternally
    var updateTime: String = definedExternally
    var readTime: String = definedExternally
    fun data(): Json = definedExternally
    fun get(fieldPath: String): Any = definedExternally
}

open external class Query {
    var firestore: Firestore = definedExternally
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
    var query: Query = definedExternally
    var docChanges: Array<DocumentChange> = definedExternally
    var docs: Array<DocumentSnapshot> = definedExternally
    var size: Number = definedExternally
    var empty: Boolean = definedExternally
    var readTime: String = definedExternally
    fun forEach(callback: (result: DocumentSnapshot) -> Unit, thisArg: Any? = definedExternally): Unit = definedExternally
}

external interface DocumentChange {
    var type: dynamic
    var doc: DocumentSnapshot
    var oldIndex: Number
    var newIndex: Number
}

external class CollectionReference : Query {
    var id: String = definedExternally
    var parent: DocumentReference? = definedExternally
    var path: String = definedExternally
    fun doc(documentPath: String? = definedExternally): DocumentReference = definedExternally
    fun add(data: Json): Promise<DocumentReference> = definedExternally
}

external class DatabaseBuilder {
    var resource: Any = definedExternally
    fun namespace(namespace: String): NamespaceBuilder = definedExternally
    fun document(path: String): DocumentBuilder = definedExternally
}

external class NamespaceBuilder {
    var resource: Any = definedExternally
    fun document(path: String): DocumentBuilder = definedExternally
}

external interface DeltaDocumentSnapshot {
    var exists: Boolean
    var ref: DocumentReference
    var id: String
    var createTime: String
    var updateTime: String
    var readTime: String
    var previous: DeltaDocumentSnapshot
    var get: (key: String) -> Any?
    fun data(): Json
}

external class DocumentBuilder {
    var resource: Any = definedExternally
    fun onWrite(handler: (event: Event<DeltaDocumentSnapshot>) -> Promise<*>): dynamic = definedExternally
    fun onCreate(handler: (event: Event<DeltaDocumentSnapshot>) -> Promise<*>): dynamic = definedExternally
    fun onUpdate(handler: (event: Event<DeltaDocumentSnapshot>) -> Promise<*>): dynamic = definedExternally
    fun onDelete(handler: (event: Event<DeltaDocumentSnapshot>) -> Promise<*>): dynamic = definedExternally
    fun onOperation(handler: Any, eventType: Any): Unit = definedExternally
}

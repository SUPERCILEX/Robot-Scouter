@file:Suppress("INTERFACE_WITH_SUPERCLASS", "OVERRIDING_FINAL_MEMBER", "RETURN_TYPE_MISMATCH_ON_OVERRIDE", "CONFLICTING_OVERLOADS", "unused")
@file:[JsQualifier("FirebaseFirestore") JsModule("@google-cloud/firestore")]
package com.supercilex.robotscouter.server.utils.types

import kotlin.js.Json
import kotlin.js.Promise

external fun setLogFunction(logger: (msg: String) -> Unit): Unit = definedExternally
external open class Firestore(options: Any? = definedExternally /* null */) {
    open fun collection(collectionPath: String): CollectionReference = definedExternally
    open fun doc(documentPath: String): DocumentReference = definedExternally
    open fun getAll(vararg documentRef: DocumentReference): Promise<Array<DocumentSnapshot>> = definedExternally
    open fun <T> runTransaction(updateFunction: (transaction: Transaction) -> Promise<T>): Promise<T> = definedExternally
    open fun batch(): WriteBatch = definedExternally
}
external open class GeoPoint(latitude: Number, longitude: Number) {
    open var latitude: Number = definedExternally
    open var longitude: Number = definedExternally
}
external open class Transaction {
    open fun get(documentRef: DocumentReference): Promise<DocumentSnapshot> = definedExternally
    open fun get(query: Query): Promise<QuerySnapshot> = definedExternally
    open fun create(documentRef: DocumentReference, data: Json): Transaction = definedExternally
    open fun set(documentRef: DocumentReference, data: Json, options: SetOptions? = definedExternally /* null */): Transaction = definedExternally
    open fun update(documentRef: DocumentReference, data: Json, precondition: Precondition? = definedExternally /* null */): Transaction = definedExternally
    open fun update(documentRef: DocumentReference, field: String, value: Any, vararg fieldsOrPrecondition: Any): Transaction = definedExternally
    open fun update(documentRef: DocumentReference, field: FieldPath, value: Any, vararg fieldsOrPrecondition: Any): Transaction = definedExternally
    open fun delete(documentRef: DocumentReference, precondition: Precondition? = definedExternally /* null */): Transaction = definedExternally
}
external open class WriteBatch {
    open fun create(documentRef: DocumentReference, data: Json): WriteBatch = definedExternally
    open fun set(documentRef: DocumentReference, data: Json, options: SetOptions? = definedExternally /* null */): WriteBatch = definedExternally
    open fun update(documentRef: DocumentReference, data: Json, precondition: Precondition? = definedExternally /* null */): WriteBatch = definedExternally
    open fun update(documentRef: DocumentReference, field: String, value: Any, vararg fieldsOrPrecondition: Any): WriteBatch = definedExternally
    open fun update(documentRef: DocumentReference, field: FieldPath, value: Any, vararg fieldsOrPrecondition: Any): WriteBatch = definedExternally
    open fun delete(documentRef: DocumentReference, precondition: Precondition? = definedExternally /* null */): WriteBatch = definedExternally
    open fun commit(): Promise<Array<WriteResult>> = definedExternally
}
external interface Precondition {
    var lastUpdateTime: String? get() = definedExternally; set(value) = definedExternally
}
external interface SetOptions {
    var merge: Boolean? get() = definedExternally; set(value) = definedExternally
}
external open class WriteResult {
    open var writeTime: String = definedExternally
}
external open class DocumentReference {
    open var id: String = definedExternally
    open var firestore: Firestore = definedExternally
    open var parent: CollectionReference = definedExternally
    open var path: String = definedExternally
    open fun collection(collectionPath: String): CollectionReference = definedExternally
    open fun create(data: Json): Promise<WriteResult> = definedExternally
    open fun set(data: Json, options: SetOptions? = definedExternally /* null */): Promise<WriteResult> = definedExternally
    open fun update(data: Json, precondition: Precondition? = definedExternally /* null */): Promise<WriteResult> = definedExternally
    open fun update(field: String, value: Any, vararg moreFieldsOrPrecondition: Any): Promise<WriteResult> = definedExternally
    open fun update(field: FieldPath, value: Any, vararg moreFieldsOrPrecondition: Any): Promise<WriteResult> = definedExternally
    open fun delete(precondition: Precondition? = definedExternally /* null */): Promise<WriteResult> = definedExternally
    open fun get(): Promise<DocumentSnapshot> = definedExternally
    open fun onSnapshot(onNext: (snapshot: DocumentSnapshot) -> Unit, onError: ((error: Error) -> Unit)? = definedExternally /* null */): () -> Unit = definedExternally
}
external open class DocumentSnapshot {
    open var exists: Boolean = definedExternally
    open var ref: DocumentReference = definedExternally
    open var id: String = definedExternally
    open var createTime: String = definedExternally
    open var updateTime: String = definedExternally
    open var readTime: String = definedExternally
    open fun data(): Json = definedExternally
    open fun get(fieldPath: String): Any = definedExternally
    open fun get(fieldPath: FieldPath): Any = definedExternally
}
external open class Query {
    open var firestore: Firestore = definedExternally
    open fun where(fieldPath: String,
                   opStr: String /* "<" */,
                   value: Any?): Query = definedExternally
//    open fun where(fieldPath: String, opStr: Any /* "<=" */, value: Any): Query = definedExternally
//    open fun where(fieldPath: String, opStr: Any /* "==" */, value: Any): Query = definedExternally
//    open fun where(fieldPath: String, opStr: Any /* ">=" */, value: Any): Query = definedExternally
//    open fun where(fieldPath: String, opStr: Any /* ">" */, value: Any): Query = definedExternally
    open fun where(fieldPath: FieldPath, opStr: Any /* "<" */, value: Any): Query = definedExternally
    open fun where(fieldPath: FieldPath, opStr: Any /* "<=" */, value: Any): Query = definedExternally
    open fun where(fieldPath: FieldPath, opStr: Any /* "==" */, value: Any): Query = definedExternally
    open fun where(fieldPath: FieldPath, opStr: Any /* ">=" */, value: Any): Query = definedExternally
    open fun where(fieldPath: FieldPath, opStr: Any /* ">" */, value: Any): Query = definedExternally
    open fun orderBy(fieldPath: String, directionStr: Any? /* "desc" */ = definedExternally /* null */): Query = definedExternally
    //    open fun orderBy(fieldPath: String, directionStr: Any? /* "asc" */ = definedExternally /* null */): Query = definedExternally
    open fun orderBy(fieldPath: FieldPath, directionStr: Any? /* "desc" */ = definedExternally /* null */): Query = definedExternally
    open fun orderBy(fieldPath: FieldPath, directionStr: Any? /* "asc" */ = definedExternally /* null */): Query = definedExternally
    open fun limit(limit: Number): Query = definedExternally
    open fun offset(offset: Number): Query = definedExternally
    open fun select(vararg field: dynamic /* String | com.supercilex.robotscouter.server.utils.types.FieldPath */): Query = definedExternally
    open fun startAt(vararg fieldValues: Any): Query = definedExternally
    open fun startAfter(vararg fieldValues: Any): Query = definedExternally
    open fun endBefore(vararg fieldValues: Any): Query = definedExternally
    open fun endAt(vararg fieldValues: Any): Query = definedExternally
    open fun get(): Promise<QuerySnapshot> = definedExternally
    open fun onSnapshot(onNext: (snapshot: QuerySnapshot) -> Unit, onError: ((error: Error) -> Unit)? = definedExternally /* null */): () -> Unit = definedExternally
}
external open class QuerySnapshot {
    open var query: Query = definedExternally
    open var docChanges: Array<DocumentChange> = definedExternally
    open var docs: Array<DocumentSnapshot> = definedExternally
    open var size: Number = definedExternally
    open var empty: Boolean = definedExternally
    open var readTime: String = definedExternally
    open fun forEach(callback: (result: DocumentSnapshot) -> Unit, thisArg: Any? = definedExternally /* null */): Unit = definedExternally
}
external interface DocumentChange {
    var type: dynamic /* Any /* "added" */ | Any /* "removed" */ | Any /* "modified" */ */
    var doc: DocumentSnapshot
    var oldIndex: Number
    var newIndex: Number
}
external open class CollectionReference : Query {
    open var id: String = definedExternally
    open var parent: DocumentReference? = definedExternally
    open var path: String = definedExternally
    open fun doc(documentPath: String? = definedExternally /* null */): DocumentReference = definedExternally
    open fun add(data: Json): Promise<DocumentReference> = definedExternally
}
external open class FieldValue {
    companion object {
        fun serverTimestamp(): FieldValue = definedExternally
        fun delete(): FieldValue = definedExternally
    }
}
external open class FieldPath(vararg fieldNames: String) {
    companion object {
        fun documentId(): FieldPath = definedExternally
    }
}

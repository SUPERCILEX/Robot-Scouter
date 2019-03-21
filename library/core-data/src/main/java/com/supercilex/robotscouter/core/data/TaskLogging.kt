package com.supercilex.robotscouter.core.data

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.supercilex.robotscouter.core.CrashLogger
import com.supercilex.robotscouter.core.logBreadcrumb

fun <T> Task<T>.logFailures(
        tag: String,
        vararg hints: Any?
): Task<T> = addOnFailureListener {
    for (hint in hints) logBreadcrumb(hint.toString())
    logBreadcrumb("Source: $tag")
    CrashLogger.invoke(it)
}

fun <T> Task<T>.logFailures(
        tag: String,
        ref: DocumentReference,
        vararg hints: Any?
) = logFailures(tag, "Path: ${ref.path}", *hints)

fun <T> Task<T>.logFailures(
        tag: String,
        refs: List<DocumentReference>,
        vararg hints: Any?
) = logFailures(tag, "Paths: ${refs.joinToString { it.path }}", *hints)

@file:Suppress("NOTHING_TO_INLINE")

package com.supercilex.robotscouter.core.data

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.supercilex.robotscouter.core.logFailures

inline fun <T> Task<T>.logFailures(
        ref: DocumentReference,
        data: Any? = null
) = logFailures("Path: ${ref.path}", "Data: $data")

inline fun <T> Task<T>.logFailures(
        refs: List<DocumentReference>,
        data: Any? = null
) = logFailures("Paths: ${refs.joinToString { it.path }}", "Data: $data")

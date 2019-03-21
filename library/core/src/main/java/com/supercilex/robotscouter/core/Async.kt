package com.supercilex.robotscouter.core

import android.app.Activity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.CancellationException
import org.jetbrains.anko.coroutines.experimental.Ref
import org.jetbrains.anko.coroutines.experimental.asReference

fun <T> Task<T>.fastAddOnSuccessListener(
        activity: Activity? = null,
        listener: (T) -> Unit
): Task<T> {
    if (isSuccessful) { // Fast path
        @Suppress("UNCHECKED_CAST") // Let the caller decide nullability
        listener(result as T)
        return this
    }

    return if (activity == null) {
        addOnSuccessListener(listener)
    } else {
        addOnSuccessListener(activity, listener)
    }
}

fun <T : LifecycleOwner> T.asLifecycleReference(
        minState: Lifecycle.State = Lifecycle.State.STARTED
) = asLifecycleReference(this, minState)

fun <T : Any> T.asLifecycleReference(
        owner: LifecycleOwner,
        minState: Lifecycle.State = Lifecycle.State.STARTED
) = LifecycleOwnerRef(asReference(), owner.asReference(), minState)

class LifecycleOwnerRef<out T : Any>(
        private val obj: Ref<T>,
        private val owner: Ref<LifecycleOwner>,
        private val minState: Lifecycle.State
) {
    suspend operator fun invoke(): T {
        if (!owner().lifecycle.currentState.isAtLeast(minState)) throw CancellationException()
        return obj()
    }
}

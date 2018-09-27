package com.supercilex.robotscouter.core

import android.app.Activity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.tasks.await
import org.jetbrains.anko.coroutines.experimental.Ref
import org.jetbrains.anko.coroutines.experimental.asReference

suspend fun <T> Task<T>.await(): T {
    val trace = generateStackTrace()
    return try {
        await()
    } catch (t: Throwable) {
        throw t.injectRoot(trace)
    }
}

inline fun <T> Task<T>.fastAddOnSuccessListener(
        activity: Activity? = null,
        crossinline listener: (T) -> Unit
): Task<T> {
    if (isSuccessful) { // Fast path
        listener(result)
        return this
    }

    val crossedListener = OnSuccessListener<T> { listener(it) }
    return if (activity == null) {
        addOnSuccessListener(crossedListener)
    } else {
        addOnSuccessListener(activity, crossedListener)
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

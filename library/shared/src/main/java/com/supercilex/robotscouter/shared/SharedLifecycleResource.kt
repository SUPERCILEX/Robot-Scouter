package com.supercilex.robotscouter.shared

import androidx.lifecycle.ViewModel
import java.util.concurrent.atomic.AtomicInteger

/**
 * Manages lifecycle events for shared objects. Used to process fragment view lifecycle events for
 * activity scoped resources.
 */
class SharedLifecycleResource : ViewModel() {
    private val counts = mutableMapOf<Class<*>, AtomicInteger>()

    fun onCreate(resource: Any) {
        counts.getOrPut(resource.javaClass) { AtomicInteger() }.incrementAndGet()
    }

    fun <T : Any> onDestroy(resource: T, cleanup: T.() -> Unit) {
        val count = counts.getValue(resource.javaClass).decrementAndGet()
        if (count == 0) cleanup(resource)
    }
}

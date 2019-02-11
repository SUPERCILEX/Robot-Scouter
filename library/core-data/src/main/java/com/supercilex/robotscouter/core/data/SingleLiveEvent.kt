package com.supercilex.robotscouter.core.data

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A LiveData that only notifies observers when new data arrives, used for events like navigation
 * and SnackBar messages.
 *
 * This avoids a common problem with events: on configuration change (like rotation) an update can
 * be emitted if the observer is active.
 *
 * This LiveData supports multiple observers per class definition, but works on a stack model. Only
 * the newest observers get updates.
 */
class SingleLiveEvent<T> : MutableLiveData<T>() {
    private val observerStatuses = ConcurrentHashMap<Class<out Observer<*>>, AtomicBoolean>()

    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        observerStatuses.putIfAbsent(observer.javaClass, AtomicBoolean())
        super.observe(owner, EventFilterObserver(observer))
    }

    override fun removeObserver(observer: Observer<in T>) = super.removeObserver(
            observer as? EventFilterObserver ?: error("Cannot manually remove observers"))

    override fun setValue(t: T?) {
        observerStatuses.values.forEach { it.set(true) }
        super.setValue(t)
    }

    private inner class EventFilterObserver(val originalObserver: Observer<in T>) : Observer<T> {
        override fun onChanged(t: T?) {
            if (observerStatuses.getValue(originalObserver.javaClass).compareAndSet(true, false)) {
                originalObserver.onChanged(t)
            }
        }
    }
}

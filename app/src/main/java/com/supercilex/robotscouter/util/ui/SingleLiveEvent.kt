package com.supercilex.robotscouter.util.ui

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer

import java.util.ArrayList
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
    private val observers = ArrayList<EventFilterObserver>()

    override fun observe(owner: LifecycleOwner, observer: Observer<T>) {
        observerStatuses.putIfAbsent(observer.javaClass, AtomicBoolean())
        EventFilterObserver(observer).let {
            observers.add(0, it)
            super.observe(owner, it)
        }
    }

    override fun removeObserver(observer: Observer<T>) {
        (observer as? EventFilterObserver ?:
                observers.map { it.originalObserver }.single { it === observer }).let {
            observers.remove(it)
            super.removeObserver(it)
        }
    }

    override fun setValue(t: T?) {
        observerStatuses.values.forEach { it.set(true) }
        super.setValue(t)
    }

    private inner class EventFilterObserver(val originalObserver: Observer<T>) : Observer<T> {
        private val newestObserver: Observer<T>
            get() = observers.map { it.originalObserver }.single {
                it.javaClass == this.originalObserver.javaClass
            }

        override fun onChanged(t: T?) {
            if (observerStatuses[originalObserver.javaClass]!!.compareAndSet(true, false)) {
                newestObserver.onChanged(t)
            }
        }
    }
}

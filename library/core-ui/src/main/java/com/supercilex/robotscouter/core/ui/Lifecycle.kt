package com.supercilex.robotscouter.core.ui

import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

inline fun <T : Any> LiveData<T>.observeNonNull(
        owner: LifecycleOwner,
        crossinline observer: (T) -> Unit
) = observe(owner, Observer { observer(checkNotNull(it)) })

fun Fragment.addViewLifecycleObserver(observer: LifecycleObserver) {
    viewLifecycleOwnerLiveData.observeForever {
        it?.lifecycle?.addObserver(observer)
    }
}

@Suppress("FunctionName") // Fake class
fun <T> Fragment.LifecycleAwareLazy(evaluator: () -> T): ReadOnlyLifecycleAwareLazy<T> =
        ReadOnlyFragmentLifecycleAwareLazy(this, evaluator)

@Suppress("FunctionName") // Fake class
fun <T : Any> Fragment.LifecycleAwareLazy(): ReadWriteLifecycleAwareLazy<T> =
        ReadWriteFragmentLifecycleAwareLazy(this)

infix fun <R : LifecycleAwareLazy<T>, T> R.onDestroy(cleanup: (T) -> Unit) = apply {
    this.cleanup = cleanup
}

interface LifecycleAwareLazy<T> {
    var cleanup: ((T) -> Unit)?
}

interface ReadOnlyLifecycleAwareLazy<T> : LifecycleAwareLazy<T>, ReadOnlyProperty<Any?, T>

interface ReadWriteLifecycleAwareLazy<T> : LifecycleAwareLazy<T>, ReadWriteProperty<Any?, T>

private abstract class LifecycleAwareLazyBase<T>(fragment: Fragment) : LifecycleAwareLazy<T>,
        DefaultLifecycleObserver {
    override var cleanup: ((T) -> Unit)? = null

    protected var value: T? = null

    init {
        fragment.addViewLifecycleObserver(this)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        value?.let { cleanup?.invoke(it) }
        value = null
    }
}

private class ReadOnlyFragmentLifecycleAwareLazy<T>(
        private val fragment: Fragment,
        private val evaluator: () -> T
) : LifecycleAwareLazyBase<T>(fragment), ReadOnlyLifecycleAwareLazy<T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        checkNotNull(fragment.view) { "${property.name} cannot be accessed before onCreateView." }
        return value ?: evaluator().also { value = it }
    }
}

private class ReadWriteFragmentLifecycleAwareLazy<T : Any>(fragment: Fragment) :
        LifecycleAwareLazyBase<T>(fragment), ReadWriteLifecycleAwareLazy<T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>) = checkNotNull(value) {
        "Property ${property.name} was not initialized or has been recycled."
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }
}

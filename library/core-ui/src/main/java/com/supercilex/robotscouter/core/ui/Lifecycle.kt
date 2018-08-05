package com.supercilex.robotscouter.core.ui

import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

inline fun <T : Any> LiveData<T>.observeNonNull(
        owner: LifecycleOwner,
        crossinline observer: (T) -> Unit
) = observe(owner, Observer { observer(checkNotNull(it)) })

@Suppress("FunctionName") // Fake class
fun <T> Fragment.LifecycleAwareLazy(evaluator: () -> T) = LifecycleAwareLazy(this, evaluator)

infix fun <T> LifecycleAwareLazy<T>.onDestroy(cleanup: (T) -> Unit) = apply {
    this.cleanup = cleanup
}

class LifecycleAwareLazy<T>(
        fragment: Fragment,
        private val evaluator: () -> T
) : ReadOnlyProperty<Any?, T>, DefaultLifecycleObserver {
    internal var cleanup: ((T) -> Unit)? = null

    private var value: T? = null

    init {
        fragment.viewLifecycleOwnerLiveData.observeForever {
            it?.lifecycle?.addObserver(this)
        }
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>) =
            value ?: evaluator().also { value = it }

    override fun onDestroy(owner: LifecycleOwner) {
        value?.let { cleanup?.invoke(it) }
        value = null
    }
}

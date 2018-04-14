package com.supercilex.robotscouter.core.data

import android.arch.lifecycle.ViewModel
import android.support.annotation.CallSuper
import java.util.concurrent.atomic.AtomicBoolean

abstract class ViewModelBase<in T> : ViewModel() {
    private val isInitialized = AtomicBoolean()

    fun init(args: T) {
        if (isInitialized.compareAndSet(false, true)) {
            onCreate(args)
        }
    }

    protected abstract fun onCreate(args: T)

    @CallSuper
    override fun onCleared() {
        isInitialized.set(false)
    }
}

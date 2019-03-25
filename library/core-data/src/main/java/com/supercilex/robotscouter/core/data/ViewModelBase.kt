package com.supercilex.robotscouter.core.data

import androidx.annotation.CallSuper
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import java.util.concurrent.atomic.AtomicBoolean

abstract class ViewModelBase<in T>(protected val state: SavedStateHandle) : ViewModel() {
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

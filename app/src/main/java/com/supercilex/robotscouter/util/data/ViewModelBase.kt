package com.supercilex.robotscouter.util.data

import android.arch.lifecycle.ViewModel
import android.support.annotation.CallSuper

abstract class ViewModelBase<in T> : ViewModel() {
    private var isInitialized = false

    fun init(args: T) {
        if (!isInitialized) {
            onCreate(args)
            isInitialized = true
        }
    }

    protected abstract fun onCreate(args: T)

    @CallSuper
    override fun onCleared() {
        isInitialized = false
    }
}

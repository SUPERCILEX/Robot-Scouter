package com.supercilex.robotscouter.util.data

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.support.annotation.CallSuper

abstract class ViewModelBase<in T>(app: Application) : AndroidViewModel(app) {
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

package com.supercilex.robotscouter.ui

import android.app.Application
import android.arch.lifecycle.AndroidViewModel

abstract class ViewModelBase<in T>(app: Application) : AndroidViewModel(app) {
    private var isInitialized = false

    fun init(args: T) {
        if (!isInitialized) {
            onCreate(args)
            isInitialized = true
        }
    }

    protected abstract fun onCreate(args: T)
}

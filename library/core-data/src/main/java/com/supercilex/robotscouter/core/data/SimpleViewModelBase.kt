package com.supercilex.robotscouter.core.data

import androidx.lifecycle.SavedStateHandle

abstract class SimpleViewModelBase(state: SavedStateHandle) : ViewModelBase<Unit?>(state) {
    fun init() = init(null)

    final override fun onCreate(args: Unit?) = onCreate()

    open fun onCreate() = Unit
}

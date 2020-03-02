package com.supercilex.robotscouter.core.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class StateHolder<T : Any>(state: T) {
    private val _liveData = MutableLiveData(state)
    val liveData: LiveData<T> get() = _liveData

    private var _value: T = state
    val value: T get() = _value

    fun update(notify: Boolean = true, block: T.() -> T) {
        synchronized(LOCK) {
            _value = block(value)
            if (notify) _liveData.postValue(value)
        }
    }

    private companion object {
        val LOCK = Object()
    }
}

package com.supercilex.robotscouter.util.data

import android.arch.lifecycle.GenericLifecycleObserver
import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LifecycleRegistry
import android.arch.lifecycle.ProcessLifecycleOwner
import com.supercilex.robotscouter.util.ui.activitiesLifecycleOwner
import com.supercilex.robotscouter.util.ui.mainHandler

/**
 * Provides lifecycle for suggested listener registration status.
 *
 * To save battery, this class provides a way to monitor when all activities have been in their
 * [Activity.onStop()] state for an extended period of time.
 *
 * @see ProcessLifecycleOwner
 */
object ListenerRegistrationLifecycleOwner : LifecycleOwner, Runnable, GenericLifecycleObserver {
    private const val TIMEOUT_IN_MILLIS = 300000L // 5 minutes

    private val registry = LifecycleRegistry(this)

    init {
        activitiesLifecycleOwner.lifecycle.addObserver(this)
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event === Lifecycle.Event.ON_START) {
            mainHandler.removeCallbacks(this)
        } else if (event === Lifecycle.Event.ON_STOP) {
            mainHandler.postDelayed(this, TIMEOUT_IN_MILLIS)
            return
        }

        registry.handleLifecycleEvent(event)
    }

    override fun run() {
        registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    override fun getLifecycle(): Lifecycle = registry
}

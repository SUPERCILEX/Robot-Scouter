package com.supercilex.robotscouter.core.data

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ProcessLifecycleOwner
import com.supercilex.robotscouter.core.mainHandler

/**
 * Provides lifecycle for suggested listener registration status.
 *
 * To save battery, this class provides a way to monitor when all activities have been in their
 * [Activity.onStop()] state for an extended period of time.
 *
 * @see ProcessLifecycleOwner
 */
object ListenerRegistrationLifecycleOwner : LifecycleOwner {
    private val registry = LifecycleRegistry(this)

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(Listener())
    }

    override fun getLifecycle(): Lifecycle = registry

    private class Listener : Runnable, LifecycleEventObserver {
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

        private companion object {
            const val TIMEOUT_IN_MILLIS = 300_000L // 5 minutes
        }
    }
}

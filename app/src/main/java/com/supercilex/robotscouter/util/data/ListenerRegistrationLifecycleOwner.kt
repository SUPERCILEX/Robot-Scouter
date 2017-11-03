package com.supercilex.robotscouter.util.data

import android.arch.core.executor.ArchTaskExecutor
import android.arch.lifecycle.DefaultLifecycleObserver
import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LifecycleRegistry
import android.arch.lifecycle.ProcessLifecycleOwner
import java.util.Timer
import java.util.TimerTask

/**
 * Provides lifecycle for suggested listener registration status.
 *
 * To save battery, this class provides a way to monitor when all activities have been in their
 * [Activity.onStop()] state for an extended period of time.
 *
 * @see ProcessLifecycleOwner
 */
object ListenerRegistrationLifecycleOwner : LifecycleOwner, DefaultLifecycleObserver {
    private const val TIMEOUT_IN_MILLIS = 60000L

    private val registry = LifecycleRegistry(this)

    private val unregisterTask
        get() = object : TimerTask() {
            override fun run() {
                ArchTaskExecutor.getInstance().executeOnMainThread {
                    registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
                }
            }
        }
    private var timeout: Timer? = null

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onStart(owner: LifecycleOwner) {
        timeout?.cancel()
        timeout = null
        registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    override fun onResume(owner: LifecycleOwner) {
        registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onPause(owner: LifecycleOwner) {
        registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    }

    override fun onStop(owner: LifecycleOwner) {
        timeout = Timer().apply {
            schedule(unregisterTask, TIMEOUT_IN_MILLIS)
        }
    }

    override fun getLifecycle(): Lifecycle = registry
}

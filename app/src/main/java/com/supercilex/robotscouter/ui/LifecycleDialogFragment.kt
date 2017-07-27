package com.supercilex.robotscouter.ui

import android.arch.lifecycle.LifecycleRegistry
import android.arch.lifecycle.LifecycleRegistryOwner
import android.support.v4.app.DialogFragment

// TODO remove once arch components are merged into support lib
abstract class LifecycleDialogFragment : DialogFragment(), LifecycleRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)

    override fun getLifecycle(): LifecycleRegistry = lifecycleRegistry
}

package com.supercilex.robotscouter.util.ui

import android.arch.lifecycle.LifecycleRegistry
import android.arch.lifecycle.LifecycleRegistryOwner
import android.content.Intent
import android.os.Build
import android.support.v7.app.AppCompatActivity

fun Intent.addNewDocumentFlags(): Intent {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
    }
    return this
}

// TODO remove once arch components are merged into support lib
abstract class LifecycleActivity : AppCompatActivity(), LifecycleRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)

    override fun getLifecycle(): LifecycleRegistry = lifecycleRegistry
}

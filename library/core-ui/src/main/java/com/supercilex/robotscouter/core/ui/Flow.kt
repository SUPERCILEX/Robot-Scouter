package com.supercilex.robotscouter.core.ui

import android.content.Intent
import android.os.Build
import androidx.recyclerview.widget.RecyclerView

fun Intent.addNewDocumentFlags(): Intent {
    if (Build.VERSION.SDK_INT >= 21) {
        addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
    } else {
        @Suppress("DEPRECATION")
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
    }
    addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
    return this
}

interface RecyclerPoolHolder {
    val recyclerPool: RecyclerView.RecycledViewPool
}

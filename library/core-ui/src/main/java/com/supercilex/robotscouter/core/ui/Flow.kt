package com.supercilex.robotscouter.core.ui

import android.content.Intent
import androidx.recyclerview.widget.RecyclerView

fun Intent.addNewDocumentFlags(): Intent {
    addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
    addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
    return this
}

interface RecyclerPoolHolder {
    val recyclerPool: RecyclerView.RecycledViewPool
}

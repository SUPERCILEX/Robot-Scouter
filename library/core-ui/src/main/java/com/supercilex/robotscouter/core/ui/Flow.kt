package com.supercilex.robotscouter.core.ui

import android.content.Intent
import androidx.recyclerview.widget.RecyclerView
import org.jetbrains.anko.newDocument

fun Intent.addNewDocumentFlags(): Intent {
    newDocument()
    addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
    return this
}

interface RecyclerPoolHolder {
    val recyclerPool: RecyclerView.RecycledViewPool
}

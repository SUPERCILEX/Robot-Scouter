package com.supercilex.robotscouter.core.ui

import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.RecyclerView

fun Intent.addNewDocumentFlags(): Intent {
    addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
    addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
    return this
}

interface OnBackPressedListener {
    /** @return true if the back press was consumed, false otherwise. */
    fun onBackPressed(): Boolean
}

interface TeamSelectionListener {
    fun onTeamSelected(args: Bundle, restoreOnConfigChange: Boolean = false)
}

interface TemplateSelectionListener {
    fun onTemplateSelected(id: String)
}

interface RecyclerPoolHolder {
    val recyclerPool: RecyclerView.RecycledViewPool
}

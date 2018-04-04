package com.supercilex.robotscouter.util.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.NavUtils
import android.support.v4.app.TaskStackBuilder
import android.support.v7.widget.RecyclerView
import com.supercilex.robotscouter.util.data.ACTION_FROM_DEEP_LINK

fun Intent.addNewDocumentFlags(): Intent {
    addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
    addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
    return this
}

fun Activity.handleUpNavigation() {
    if (NavUtils.shouldUpRecreateTask(this, NavUtils.getParentActivityIntent(this)!!)
            || intent.action == ACTION_FROM_DEEP_LINK) {
        TaskStackBuilder.create(this).addParentStack(this).startActivities()
        finish()
    } else {
        NavUtils.navigateUpFromSameTask(this)
    }
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

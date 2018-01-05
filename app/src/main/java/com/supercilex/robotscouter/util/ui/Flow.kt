package com.supercilex.robotscouter.util.ui

import android.app.Activity
import android.arch.lifecycle.LiveData
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v4.app.NavUtils
import android.support.v4.app.TaskStackBuilder
import android.support.v7.widget.RecyclerView
import com.google.android.gms.tasks.Task
import com.supercilex.robotscouter.util.data.ACTION_FROM_DEEP_LINK

fun Intent.addNewDocumentFlags(): Intent {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
    }
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

fun <T> Task<T>.asLiveData(): LiveData<Task<T>> = SingleLiveEvent<Task<T>>().apply {
    addOnCompleteListener {
        value = it
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

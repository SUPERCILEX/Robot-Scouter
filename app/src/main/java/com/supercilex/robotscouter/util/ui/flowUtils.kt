package com.supercilex.robotscouter.util.ui

import android.arch.lifecycle.LiveData
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import com.google.android.gms.tasks.Task

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

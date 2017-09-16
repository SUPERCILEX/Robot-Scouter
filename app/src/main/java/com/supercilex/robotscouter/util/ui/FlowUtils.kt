package com.supercilex.robotscouter.util.ui

import android.os.Bundle

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

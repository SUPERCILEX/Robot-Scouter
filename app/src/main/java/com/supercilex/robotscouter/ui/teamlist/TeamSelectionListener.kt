package com.supercilex.robotscouter.ui.teamlist

import android.os.Bundle

interface TeamSelectionListener {
    fun onTeamSelected(args: Bundle, restoreOnConfigChange: Boolean = false)
}

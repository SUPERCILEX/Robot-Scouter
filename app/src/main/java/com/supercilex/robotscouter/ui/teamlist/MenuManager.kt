package com.supercilex.robotscouter.ui.teamlist

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem

interface MenuManager : OnBackPressedListener {
    fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater)

    /** @see android.app.Activity.onOptionsItemSelected */
    fun onOptionsItemSelected(item: MenuItem): Boolean

    fun resetMenu()

    fun saveState(outState: Bundle)

    fun restoreState(savedInstanceState: Bundle)
}

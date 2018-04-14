package com.supercilex.robotscouter.feature.settings

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import com.supercilex.robotscouter.core.ui.ActivityBase
import com.supercilex.robotscouter.core.ui.OnBackPressedListener
import com.supercilex.robotscouter.shared.handleUpNavigation
import org.jetbrains.anko.intentFor

class SettingsActivity : ActivityBase() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.RobotScouter_Settings)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .add(R.id.settings, SettingsFragment.newInstance(), SettingsFragment.TAG)
                    .commit()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            val handledBack = supportFragmentManager.fragments
                    .any { it is OnBackPressedListener && it.onBackPressed() }
            if (handledBack) return true

            handleUpNavigation()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    companion object {
        fun show(context: Context) = context.startActivity(context.intentFor<SettingsActivity>())
    }
}

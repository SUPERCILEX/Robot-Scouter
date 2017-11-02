package com.supercilex.robotscouter.ui.settings

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.util.ui.OnBackPressedListener
import com.supercilex.robotscouter.util.ui.handleUpNavigation
import org.jetbrains.anko.intentFor

class SettingsActivity : AppCompatActivity() {
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

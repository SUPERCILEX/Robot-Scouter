package com.supercilex.robotscouter.feature.settings

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.supercilex.robotscouter.Bridge
import com.supercilex.robotscouter.SettingsActivityCompanion
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.ui.ActivityBase
import com.supercilex.robotscouter.shared.handleUpNavigation

@Bridge
class SettingsActivity : ActivityBase(),
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.RobotScouter_Settings)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                add(R.id.settings, SettingsFragment.newInstance(), SettingsFragment.TAG)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack()
            } else {
                handleUpNavigation()
            }

            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onPreferenceStartFragment(
            caller: PreferenceFragmentCompat,
            pref: Preference
    ): Boolean {
        when (pref.key) {
            LicensesFragment.KEY_LICENSES -> supportFragmentManager.commit {
                setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                replace(R.id.settings, LicensesFragment.newInstance())
                addToBackStack(null)
            }
            else -> return false
        }

        return true
    }

    companion object : SettingsActivityCompanion {
        override fun createIntent() = Intent(RobotScouter, SettingsActivity::class.java)
    }
}

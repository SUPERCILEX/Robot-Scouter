package com.supercilex.robotscouter.ui.settings

import android.os.Bundle
import android.support.v7.preference.PreferenceFragmentCompat
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.util.data.prefs

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.app_preferences)
        preferenceManager.preferenceDataStore = prefs
    }
}

package com.supercilex.robotscouter.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.content.res.AppCompatResources
import android.support.v7.preference.ListPreference
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import android.support.v7.preference.PreferenceGroup
import android.support.v7.preference.PreferenceGroupAdapter
import android.support.v7.preference.PreferenceScreen
import android.support.v7.preference.PreferenceViewHolder
import android.support.v7.widget.RecyclerView
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.widget.TextView
import android.widget.Toast
import com.firebase.ui.auth.AuthUI
import com.google.firebase.appindexing.FirebaseAppIndex
import com.google.firebase.auth.FirebaseAuth
import com.supercilex.robotscouter.BuildConfig
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.ui.teamlist.LicensesFragment
import com.supercilex.robotscouter.util.data.clearPrefs
import com.supercilex.robotscouter.util.data.prefs
import com.supercilex.robotscouter.util.getDebugInfo
import com.supercilex.robotscouter.util.launchUrl

class SettingsFragment : PreferenceFragmentCompat(),
        Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = prefs
        addPreferencesFromResource(R.xml.app_preferences)
        onPreferenceChange(preferenceScreen, null)
    }

    override fun onCreateAdapter(preferenceScreen: PreferenceScreen): RecyclerView.Adapter<*> {
        return object : PreferenceGroupAdapter(preferenceScreen) {
            override fun onBindViewHolder(holder: PreferenceViewHolder, position: Int) {
                super.onBindViewHolder(holder, position)
                if (getItem(position).key == "about") {
                    (holder.findViewById(R.id.about) as TextView).movementMethod =
                            LinkMovementMethod.getInstance()
                }
            }
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        preference.onPreferenceChangeListener = this
        preference.onPreferenceClickListener = this

        when (preference) {
            is PreferenceGroup -> for (i in 0 until preference.preferenceCount) {
                onPreferenceChange(preference.getPreference(i), null)
            }
            is ListPreference -> {
                if (preference.value == null) {
                    preference.isPersistent = false
                    preference.value = Preference::class.java.getDeclaredField("mDefaultValue")
                            .apply { isAccessible = true }
                            .get(preference).toString()
                    preference.isPersistent = true
                }
            }
            else -> if (preference.key == "version") preference.summary = BuildConfig.VERSION_NAME
        }

        preference.icon?.let {
            val value = TypedValue()
            if (!context.theme.resolveAttribute(android.R.attr.textColorSecondary, value, true)) {
                return@let
            }

            DrawableCompat.setTint(
                    it,
                    AppCompatResources.getColorStateList(context, value.resourceId).defaultColor)
        }

        return true
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        when (preference.key) {
            "reset_tutorials" -> {
                clearPrefs()
                activity.finish()
            }
            "sign_out" -> AuthUI.getInstance()
                    .signOut(activity)
                    .addOnSuccessListener {
                        FirebaseAuth.getInstance().signInAnonymously()
                        FirebaseAppIndex.getInstance().removeAll()
                        activity.finish()
                    }
            "release_notes" -> launchUrl(
                    context, Uri.parse("https://github.com/SUPERCILEX/Robot-Scouter/releases"))
            "version" -> {
                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip =
                        ClipData.newPlainText(getString(R.string.debug_info_name), getDebugInfo())
                Toast.makeText(context, R.string.debug_info_copied, Toast.LENGTH_SHORT).show()
            }
            "licenses" -> fragmentManager.beginTransaction()
                    .setCustomAnimations(
                            R.anim.fade_in,
                            R.anim.fade_out,
                            R.anim.fade_in,
                            R.anim.fade_out)
                    .replace(R.id.settings, LicensesFragment.newInstance())
                    .addToBackStack(null)
                    .commit()
            else -> return false
        }
        return true
    }

    companion object {
        const val TAG = "SettingsFragment"

        fun newInstance() = SettingsFragment()
    }
}

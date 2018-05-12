package com.supercilex.robotscouter.feature.settings

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.content.res.AppCompatResources
import android.support.v7.preference.ListPreference
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceGroup
import android.support.v7.preference.PreferenceGroupAdapter
import android.support.v7.preference.PreferenceScreen
import android.support.v7.preference.PreferenceViewHolder
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.widget.TextView
import androidx.core.net.toUri
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.appindexing.FirebaseAppIndex
import com.google.firebase.auth.FirebaseAuth
import com.supercilex.robotscouter.common.FIRESTORE_PREF_DEFAULT_TEMPLATE_ID
import com.supercilex.robotscouter.core.data.cancelAllJobs
import com.supercilex.robotscouter.core.data.clearPrefs
import com.supercilex.robotscouter.core.data.debugInfo
import com.supercilex.robotscouter.core.data.isFullUser
import com.supercilex.robotscouter.core.data.isSignedIn
import com.supercilex.robotscouter.core.data.logLoginEvent
import com.supercilex.robotscouter.core.data.prefStore
import com.supercilex.robotscouter.core.fullVersionName
import com.supercilex.robotscouter.core.logFailures
import com.supercilex.robotscouter.core.ui.PreferenceFragmentBase
import com.supercilex.robotscouter.core.ui.TemplateSelectionListener
import com.supercilex.robotscouter.core.unsafeLazy
import com.supercilex.robotscouter.shared.client.RC_SIGN_IN
import com.supercilex.robotscouter.shared.client.startSignIn
import com.supercilex.robotscouter.shared.launchUrl
import org.jetbrains.anko.support.v4.toast

internal class SettingsFragment : PreferenceFragmentBase(),
        TemplateSelectionListener,
        Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {
    private val settingsModel by unsafeLazy {
        ViewModelProviders.of(this).get(SettingsViewModel::class.java)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        settingsModel.init(null)
        settingsModel.signOutListener.observe(this, Observer {
            if (it == null) {
                FirebaseAuth.getInstance().signInAnonymously()
                FirebaseAppIndex.getInstance().removeAll().logFailures()
                requireActivity().finish()
            } else {
                toast(R.string.error_unknown)
            }
        })
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // In a rare edge case, the user can be signed out before the fragment dies
        if (!isSignedIn) return

        preferenceManager.preferenceDataStore = prefStore
        addPreferencesFromResource(R.xml.app_preferences)
        onPreferenceChange(preferenceScreen, null)
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference.key == FIRESTORE_PREF_DEFAULT_TEMPLATE_ID) {
            SettingsTemplateSelectorDialog.show(childFragmentManager)
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    override fun onCreateAdapter(
            preferenceScreen: PreferenceScreen
    ) = object : PreferenceGroupAdapter(preferenceScreen) {
        override fun onBindViewHolder(holder: PreferenceViewHolder, position: Int) {
            super.onBindViewHolder(holder, position)
            if (getItem(position).key == "about") {
                (holder.findViewById(R.id.about) as TextView).apply {
                    text = getString(R.string.settings_pref_about_summary, "\uD83D\uDC96").trim()
                    movementMethod = LinkMovementMethod.getInstance()
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
                    preference.apply {
                        isPersistent = false
                        value = Preference::class.java.getDeclaredField("mDefaultValue")
                                .apply { isAccessible = true }
                                .get(preference)?.toString()
                        isPersistent = true
                    }
                }
            }
            else -> when (preference.key) {
                KEY_LINK_ACCOUNT, KEY_SIGN_OUT -> preference.isVisible = isFullUser
                KEY_VERSION -> preference.summary = fullVersionName
            }
        }

        preference.icon?.let {
            val value = TypedValue()
            if (!preference.context.theme
                    .resolveAttribute(android.R.attr.textColorSecondary, value, true)) {
                return@let
            }

            DrawableCompat.setTint(it, AppCompatResources.getColorStateList(
                    preference.context, value.resourceId).defaultColor)
        }

        return true
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        val activity = requireActivity()
        when (preference.key) {
            KEY_RESET_TUTORIALS -> {
                clearPrefs()
                activity.finish()
            }
            KEY_LINK_ACCOUNT -> startSignIn()
            KEY_SIGN_OUT -> {
                cancelAllJobs()
                settingsModel.signOut()
            }
            KEY_RELEASE_NOTES -> launchUrl(
                    activity,
                    "https://github.com/SUPERCILEX/Robot-Scouter/releases".toUri()
            )
            KEY_TRANSLATE -> launchUrl(
                    activity,
                    "https://www.transifex.com/supercilex/robot-scouter/".toUri()
            )
            KEY_VERSION -> {
                (activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip =
                        ClipData.newPlainText(
                                getString(R.string.settings_debug_info_title), debugInfo)
                toast(R.string.settings_debug_info_copied_message)
            }
            KEY_LICENSES -> requireFragmentManager().beginTransaction()
                    .setCustomAnimations(
                            android.R.animator.fade_in,
                            android.R.animator.fade_out,
                            android.R.animator.fade_in,
                            android.R.animator.fade_out
                    )
                    .replace(R.id.settings, LicensesFragment.newInstance())
                    .addToBackStack(null)
                    .commit()
            else -> return false
        }
        return true
    }

    override fun onTemplateSelected(id: String) {
        (preferenceScreen.findPreference(FIRESTORE_PREF_DEFAULT_TEMPLATE_ID) as ListPreference)
                .value = id
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == Activity.RESULT_OK) {
                toast(R.string.signed_in_message)
                logLoginEvent()
                requireActivity().finish()
            } else {
                val response = IdpResponse.fromResultIntent(data) ?: return

                if (response.error?.errorCode == ErrorCodes.NO_NETWORK) {
                    toast(R.string.no_connection)
                    return
                }

                toast(R.string.sign_in_failed_message)
            }
        }
    }

    companion object {
        const val TAG = "SettingsFragment"

        private const val KEY_RESET_TUTORIALS = "reset_tutorials"
        private const val KEY_LINK_ACCOUNT = "link_account"
        private const val KEY_SIGN_OUT = "sign_out"
        private const val KEY_RELEASE_NOTES = "release_notes"
        private const val KEY_TRANSLATE = "translate"
        private const val KEY_VERSION = "version"
        private const val KEY_LICENSES = "licenses"

        fun newInstance() = SettingsFragment()
    }
}

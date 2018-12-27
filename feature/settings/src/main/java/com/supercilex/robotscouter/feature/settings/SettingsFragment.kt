package com.supercilex.robotscouter.feature.settings

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.getSystemService
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.net.toUri
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceGroupAdapter
import androidx.preference.PreferenceScreen
import androidx.preference.PreferenceViewHolder
import androidx.preference.forEach
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.supercilex.robotscouter.core.data.clearPrefs
import com.supercilex.robotscouter.core.data.debugInfo
import com.supercilex.robotscouter.core.data.isFullUser
import com.supercilex.robotscouter.core.data.isSignedIn
import com.supercilex.robotscouter.core.data.logLoginEvent
import com.supercilex.robotscouter.core.data.prefStore
import com.supercilex.robotscouter.core.fullVersionName
import com.supercilex.robotscouter.core.ui.PreferenceFragmentBase
import com.supercilex.robotscouter.shared.client.RC_SIGN_IN
import com.supercilex.robotscouter.shared.client.startLinkingSignIn
import com.supercilex.robotscouter.shared.launchUrl
import org.jetbrains.anko.support.v4.toast
import com.supercilex.robotscouter.R as RC

internal class SettingsFragment : PreferenceFragmentBase(),
        Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {
    private val settingsModel by viewModels<SettingsViewModel>()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        settingsModel.init(null)
        settingsModel.signOutListener.observe(this) {
            if (it == null) {
                FirebaseAuth.getInstance().signInAnonymously()
                requireActivity().finish()
            } else {
                toast(RC.string.error_unknown)
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // In a rare edge case, the user can be signed out before the fragment dies
        if (!isSignedIn) return

        preferenceManager.preferenceDataStore = prefStore
        addPreferencesFromResource(R.xml.app_preferences)
        onPreferenceChange(preferenceScreen, null)
    }

    override fun onCreateAdapter(
            preferenceScreen: PreferenceScreen
    ) = InjectingAdapter(preferenceScreen)

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        preference.onPreferenceChangeListener = this
        preference.onPreferenceClickListener = this

        when (preference) {
            is PreferenceGroup -> preference.forEach {
                onPreferenceChange(it, null)
            }
            else -> when (preference.key) {
                KEY_LINK_ACCOUNT, KEY_SIGN_OUT -> preference.isVisible = isFullUser
                KEY_ABOUT -> preference.title =
                        resources.getText(R.string.settings_pref_about_summary).trim()
                KEY_VERSION -> preference.summary = fullVersionName
            }
        }

        preference.icon?.let {
            val value = TypedValue()
            val resolved = preference.context.theme
                    .resolveAttribute(android.R.attr.textColorSecondary, value, true)
            if (!resolved) return@let

            DrawableCompat.setTint(it, AppCompatResources.getColorStateList(
                    preference.context, value.resourceId).defaultColor)
        }

        return true
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        val activity = requireActivity()
        when (preference.key) {
            KEY_RESET_PREFS -> {
                clearPrefs()
                activity.finish()
            }
            KEY_LINK_ACCOUNT -> startLinkingSignIn()
            KEY_SIGN_OUT -> settingsModel.signOut()
            KEY_RELEASE_NOTES -> launchUrl(
                    activity,
                    "https://github.com/SUPERCILEX/Robot-Scouter/releases".toUri()
            )
            KEY_TRANSLATE -> launchUrl(
                    activity,
                    "https://www.transifex.com/supercilex/robot-scouter/".toUri()
            )
            KEY_VERSION -> {
                checkNotNull(activity.getSystemService<ClipboardManager>()).primaryClip =
                        ClipData.newPlainText(
                                getString(R.string.settings_debug_info_title), debugInfo)
                toast(R.string.settings_debug_info_copied_message)
            }
            else -> return false
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == Activity.RESULT_OK) {
                toast(RC.string.signed_in_message)
                logLoginEvent()
                requireActivity().finish()
            } else {
                val response = IdpResponse.fromResultIntent(data) ?: return

                if (response.error?.errorCode == ErrorCodes.NO_NETWORK) {
                    toast(RC.string.no_connection)
                    return
                }

                toast(RC.string.sign_in_failed_message)
            }
        }
    }

    class InjectingAdapter(
            preferenceScreen: PreferenceScreen
    ) : PreferenceGroupAdapter(preferenceScreen) {
        override fun onBindViewHolder(holder: PreferenceViewHolder, position: Int) {
            super.onBindViewHolder(holder, position)
            if (getItem(position).key == KEY_ABOUT) {
                holder.itemView.isClickable = false
                (holder.findViewById(android.R.id.title) as TextView)
                        .movementMethod = LinkMovementMethod.getInstance()
            }
        }
    }

    companion object {
        const val TAG = "SettingsFragment"

        private const val KEY_RESET_PREFS = "reset_prefs"
        private const val KEY_LINK_ACCOUNT = "link_account"
        private const val KEY_SIGN_OUT = "sign_out"
        private const val KEY_ABOUT = "about"
        private const val KEY_RELEASE_NOTES = "release_notes"
        private const val KEY_TRANSLATE = "translate"
        private const val KEY_VERSION = "version"

        fun newInstance() = SettingsFragment()
    }
}

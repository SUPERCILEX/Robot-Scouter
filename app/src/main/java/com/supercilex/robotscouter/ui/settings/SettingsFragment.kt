package com.supercilex.robotscouter.ui.settings

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.appindexing.FirebaseAppIndex
import com.google.firebase.auth.FirebaseAuth
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.util.FIRESTORE_PREF_DEFAULT_TEMPLATE_ID
import com.supercilex.robotscouter.util.RC_SIGN_IN
import com.supercilex.robotscouter.util.data.clearPrefs
import com.supercilex.robotscouter.util.data.defaultTemplateId
import com.supercilex.robotscouter.util.data.prefs
import com.supercilex.robotscouter.util.debugInfo
import com.supercilex.robotscouter.util.fullVersionName
import com.supercilex.robotscouter.util.isFullUser
import com.supercilex.robotscouter.util.launchUrl
import com.supercilex.robotscouter.util.logLoginEvent
import com.supercilex.robotscouter.util.signIn
import com.supercilex.robotscouter.util.ui.TemplateSelectionListener
import org.jetbrains.anko.support.v4.toast

class SettingsFragment : PreferenceFragmentCompat(),
        TemplateSelectionListener,
        Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = prefs
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

    override fun onCreateAdapter(preferenceScreen: PreferenceScreen): RecyclerView.Adapter<*> {
        return object : PreferenceGroupAdapter(preferenceScreen) {
            override fun onBindViewHolder(holder: PreferenceViewHolder, position: Int) {
                super.onBindViewHolder(holder, position)
                if (getItem(position).key == "about") {
                    (holder.findViewById(R.id.about) as TextView).apply {
                        text = getString(R.string.settings_pref_about_summary, "\uD83D\uDC96")
                        movementMethod = LinkMovementMethod.getInstance()
                    }
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
        val activity = activity!!
        when (preference.key) {
            KEY_RESET_TUTORIALS -> {
                clearPrefs()
                activity.finish()
            }
            KEY_LINK_ACCOUNT -> signIn(this)
            KEY_SIGN_OUT -> AuthUI.getInstance()
                    .signOut(activity)
                    .addOnSuccessListener {
                        FirebaseAuth.getInstance().signInAnonymously()
                        FirebaseAppIndex.getInstance().removeAll()
                        activity.finish()
                    }
            KEY_RELEASE_NOTES -> launchUrl(
                    activity,
                    Uri.parse("https://github.com/SUPERCILEX/Robot-Scouter/releases")
            )
            KEY_TRANSLATE -> launchUrl(
                    activity,
                    Uri.parse("https://www.transifex.com/supercilex/robot-scouter/")
            )
            KEY_VERSION -> {
                (activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip =
                        ClipData.newPlainText(
                                getString(R.string.settings_debug_info_title), debugInfo)
                toast(R.string.settings_debug_info_copied_message)
            }
            KEY_LICENSES -> fragmentManager!!.beginTransaction()
                    .setCustomAnimations(
                            R.anim.fade_in,
                            R.anim.fade_out,
                            R.anim.fade_in,
                            R.anim.fade_out
                    )
                    .replace(R.id.settings, LicensesFragment.newInstance())
                    .addToBackStack(null)
                    .commit()
            else -> return false
        }
        return true
    }

    override fun onTemplateSelected(id: String) {
        defaultTemplateId = id
        (preferenceScreen.findPreference(FIRESTORE_PREF_DEFAULT_TEMPLATE_ID) as ListPreference)
                .value = id
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == Activity.RESULT_OK) {
                toast(R.string.team_signed_in_message)
                logLoginEvent()
                activity!!.finish()
            } else {
                val response: IdpResponse = IdpResponse.fromResultIntent(data) ?: return

                if (response.errorCode == ErrorCodes.NO_NETWORK) {
                    toast(R.string.no_connection)
                    return
                }

                toast(R.string.team_sign_in_failed_message)
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

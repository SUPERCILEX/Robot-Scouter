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
import android.widget.Toast
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.appindexing.FirebaseAppIndex
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.supercilex.robotscouter.BuildConfig
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.User
import com.supercilex.robotscouter.util.FIREBASE_PREF_DEFAULT_TEMPLATE_KEY
import com.supercilex.robotscouter.util.RC_SIGN_IN
import com.supercilex.robotscouter.util.data.clearPrefs
import com.supercilex.robotscouter.util.data.defaultTemplateKey
import com.supercilex.robotscouter.util.data.model.add
import com.supercilex.robotscouter.util.data.prefs
import com.supercilex.robotscouter.util.getDebugInfo
import com.supercilex.robotscouter.util.isFullUser
import com.supercilex.robotscouter.util.launchUrl
import com.supercilex.robotscouter.util.logLoginEvent
import com.supercilex.robotscouter.util.signIn
import com.supercilex.robotscouter.util.ui.TemplateSelectionListener
import com.supercilex.robotscouter.util.uid
import com.supercilex.robotscouter.util.user

class SettingsFragment : PreferenceFragmentCompat(),
        TemplateSelectionListener,
        Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = prefs
        addPreferencesFromResource(R.xml.app_preferences)
        onPreferenceChange(preferenceScreen, null)
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference.key == FIREBASE_PREF_DEFAULT_TEMPLATE_KEY) {
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
                "link_account", "sign_out" -> preference.isVisible = isFullUser
                "version" -> preference.summary = BuildConfig.VERSION_NAME
            }
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
            "link_account" -> signIn(this)
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

    override fun onTemplateSelected(key: String) {
        defaultTemplateKey = key
        (preferenceScreen.findPreference(FIREBASE_PREF_DEFAULT_TEMPLATE_KEY) as ListPreference)
                .value = key
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(context, R.string.signed_in, Toast.LENGTH_SHORT).show()

                val user: FirebaseUser = user!!
                User(uid!!, user.email, user.displayName, user.photoUrl).add()

                logLoginEvent()

                activity.finish()
            } else {
                val response: IdpResponse = IdpResponse.fromResultIntent(data) ?: return

                if (response.errorCode == ErrorCodes.NO_NETWORK) {
                    Toast.makeText(context, R.string.no_connection, Toast.LENGTH_SHORT).show()
                    return
                }

                Toast.makeText(context, R.string.sign_in_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val TAG = "SettingsFragment"

        fun newInstance() = SettingsFragment()
    }
}

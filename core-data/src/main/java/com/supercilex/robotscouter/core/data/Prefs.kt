package com.supercilex.robotscouter.core.data

import android.content.Context
import android.content.SharedPreferences
import android.support.v7.app.AppCompatDelegate
import android.support.v7.preference.PreferenceDataStore
import androidx.core.content.edit
import com.firebase.ui.common.ChangeEventType
import com.firebase.ui.firestore.ObservableSnapshotArray
import com.google.firebase.firestore.DocumentSnapshot
import com.supercilex.robotscouter.common.FIRESTORE_PREFS
import com.supercilex.robotscouter.common.FIRESTORE_PREF_DEFAULT_TEMPLATE_ID
import com.supercilex.robotscouter.common.FIRESTORE_PREF_HAS_SHOWN_ADD_TEAM_TUTORIAL
import com.supercilex.robotscouter.common.FIRESTORE_PREF_HAS_SHOWN_SIGN_IN_TUTORIAL
import com.supercilex.robotscouter.common.FIRESTORE_PREF_LOCK_TEMPLATES
import com.supercilex.robotscouter.common.FIRESTORE_PREF_NIGHT_MODE
import com.supercilex.robotscouter.common.FIRESTORE_PREF_SHOULD_SHOW_RATING_DIALOG
import com.supercilex.robotscouter.common.FIRESTORE_PREF_UPLOAD_MEDIA_TO_TBA
import com.supercilex.robotscouter.common.FIRESTORE_VALUE
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.data.model.updateTemplateId
import com.supercilex.robotscouter.core.data.model.userPrefs
import com.supercilex.robotscouter.core.logFailures
import com.supercilex.robotscouter.core.model.TemplateType
import kotlinx.coroutines.experimental.async

private val localPrefs: SharedPreferences by lazy {
    RobotScouter.getSharedPreferences(FIRESTORE_PREFS, Context.MODE_PRIVATE)
}

val prefStore = object : PreferenceDataStore() {
    override fun putString(key: String, value: String?) {
        if (value != null) {
            val ref = userPrefs.document(key)
            ref.set(mapOf(FIRESTORE_VALUE to value)).logFailures(ref, value)
        }
    }

    override fun getString(key: String, defValue: String?): String? =
            localPrefs.getString(key, defValue)

    override fun putBoolean(key: String, value: Boolean) {
        val ref = userPrefs.document(key)
        ref.set(mapOf(FIRESTORE_VALUE to value)).logFailures(ref, value)
    }

    override fun getBoolean(key: String, defValue: Boolean): Boolean =
            localPrefs.getBoolean(key, defValue)
}

var defaultTemplateId: String
    get() = prefStore.getString(
            FIRESTORE_PREF_DEFAULT_TEMPLATE_ID,
            TemplateType.DEFAULT.id.toString()
    )!!
    set(value) {
        logUpdateDefaultTemplateId(value)
        prefStore.putString(FIRESTORE_PREF_DEFAULT_TEMPLATE_ID, value)
    }

@get:AppCompatDelegate.NightMode
val nightMode: Int
    get() {
        val mode = prefStore.getString(FIRESTORE_PREF_NIGHT_MODE, "auto")
        return when (mode) {
            "auto" -> AppCompatDelegate.MODE_NIGHT_AUTO
            "yes" -> AppCompatDelegate.MODE_NIGHT_YES
            "no" -> AppCompatDelegate.MODE_NIGHT_NO
            else -> error("Unknown night mode value: $mode")
        }
    }

val isTemplateEditingAllowed get() = !prefStore.getBoolean(FIRESTORE_PREF_LOCK_TEMPLATES, false)

val shouldAskToUploadMediaToTba: Boolean
    get() = prefStore.getString(FIRESTORE_PREF_UPLOAD_MEDIA_TO_TBA, "ask") == "ask"

var shouldUploadMediaToTba: Boolean
    get() = prefStore.getString(FIRESTORE_PREF_UPLOAD_MEDIA_TO_TBA, "ask") == "yes"
    set(value) = prefStore.putString(FIRESTORE_PREF_UPLOAD_MEDIA_TO_TBA, if (value) "yes" else "no")

var hasShownAddTeamTutorial: Boolean
    get() = prefStore.getBoolean(FIRESTORE_PREF_HAS_SHOWN_ADD_TEAM_TUTORIAL, false)
    set(value) = prefStore.putBoolean(FIRESTORE_PREF_HAS_SHOWN_ADD_TEAM_TUTORIAL, value)

var hasShownSignInTutorial: Boolean
    get() = prefStore.getBoolean(FIRESTORE_PREF_HAS_SHOWN_SIGN_IN_TUTORIAL, false)
    set(value) = prefStore.putBoolean(FIRESTORE_PREF_HAS_SHOWN_SIGN_IN_TUTORIAL, value)

var shouldShowRatingDialog: Boolean
    get() = showRatingDialog && prefStore.getBoolean(FIRESTORE_PREF_SHOULD_SHOW_RATING_DIALOG, true)
    set(value) = prefStore.putBoolean(FIRESTORE_PREF_SHOULD_SHOW_RATING_DIALOG, value)

private val prefUpdater = object : ChangeEventListenerBase {
    override fun onChildChanged(
            type: ChangeEventType,
            snapshot: DocumentSnapshot,
            newIndex: Int,
            oldIndex: Int
    ) {
        val id = snapshot.id

        if (type == ChangeEventType.ADDED || type == ChangeEventType.CHANGED) {
            var hasDefaultTemplateChanged = false

            localPrefs.edit {
                when (id) {
                    FIRESTORE_PREF_LOCK_TEMPLATES,
                    FIRESTORE_PREF_HAS_SHOWN_ADD_TEAM_TUTORIAL,
                    FIRESTORE_PREF_HAS_SHOWN_SIGN_IN_TUTORIAL,
                    FIRESTORE_PREF_SHOULD_SHOW_RATING_DIALOG
                    -> putBoolean(id, prefs[newIndex] as Boolean)

                    FIRESTORE_PREF_DEFAULT_TEMPLATE_ID,
                    FIRESTORE_PREF_NIGHT_MODE,
                    FIRESTORE_PREF_UPLOAD_MEDIA_TO_TBA
                    -> {
                        val value = prefs[newIndex] as String

                        hasDefaultTemplateChanged = id == FIRESTORE_PREF_DEFAULT_TEMPLATE_ID
                                && defaultTemplateId != value

                        putString(id, value)
                    }
                }
            }

            if (hasDefaultTemplateChanged) updateTeamTemplateIds()
        } else if (type == ChangeEventType.REMOVED) {
            localPrefs.edit { remove(id) }
        }
    }
}

fun initPrefs() {
    prefs.addChangeEventListener(prefUpdater)
}

fun <T> ObservableSnapshotArray<*>.getPrefOrDefault(id: String, defValue: T): T {
    for (i in 0..lastIndex) {
        @Suppress("UNCHECKED_CAST") // Trust the client
        if (getSnapshot(i).id == id) return get(i) as T
    }
    return defValue
}

fun clearPrefs() {
    for ((key, value) in localPrefs.all.entries) {
        when (value) {
            is Boolean -> prefStore.putBoolean(key, false)
            is String -> prefStore.putString(key, null)
            else -> error("Unknown value type: ${value?.let { it::class.java }}")
        }
    }
    clearLocalPrefs()
}

private fun clearLocalPrefs() = localPrefs.edit { clear() }

private fun updateTeamTemplateIds() {
    async {
        for (team in teams.waitForChange()) team.updateTemplateId(defaultTemplateId)
    }.logFailures()
}

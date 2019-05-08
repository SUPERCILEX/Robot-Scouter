package com.supercilex.robotscouter.core.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.preference.PreferenceDataStore
import com.firebase.ui.common.ChangeEventType
import com.firebase.ui.firestore.ObservableSnapshotArray
import com.firebase.ui.firestore.SnapshotParser
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
import com.supercilex.robotscouter.core.InvocationMarker
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.data.model.updateTemplateId
import com.supercilex.robotscouter.core.data.model.userPrefs
import com.supercilex.robotscouter.core.logBreadcrumb
import com.supercilex.robotscouter.core.model.TemplateType
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

internal val prefParser = SnapshotParser<Any?> {
    when (it.id) {
        FIRESTORE_PREF_LOCK_TEMPLATES,
        FIRESTORE_PREF_HAS_SHOWN_ADD_TEAM_TUTORIAL,
        FIRESTORE_PREF_HAS_SHOWN_SIGN_IN_TUTORIAL,
        FIRESTORE_PREF_SHOULD_SHOW_RATING_DIALOG
        -> checkNotNull(it.getBoolean(FIRESTORE_VALUE))

        FIRESTORE_PREF_DEFAULT_TEMPLATE_ID,
        FIRESTORE_PREF_NIGHT_MODE,
        FIRESTORE_PREF_UPLOAD_MEDIA_TO_TBA
        -> checkNotNull(it.getString(FIRESTORE_VALUE))

        else -> it
    }
}

val prefStore = object : PreferenceDataStore() {
    override fun putString(key: String, value: String?) {
        if (value == null || !isSignedIn) return

        val ref = userPrefs.document(key)
        ref.set(mapOf(FIRESTORE_VALUE to value)).logFailures("prefStore:putS", ref, value)
    }

    override fun getString(key: String, defValue: String?): String? =
            localPrefs.getString(key, defValue)

    override fun putBoolean(key: String, value: Boolean) {
        if (!isSignedIn) return

        val ref = userPrefs.document(key)
        ref.set(mapOf(FIRESTORE_VALUE to value)).logFailures("prefStore:putB", ref, value)
    }

    override fun getBoolean(key: String, defValue: Boolean): Boolean =
            localPrefs.getBoolean(key, defValue)
}

var defaultTemplateId: String
    get() = checkNotNull(prefStore.getString(
            FIRESTORE_PREF_DEFAULT_TEMPLATE_ID,
            TemplateType.DEFAULT.id.toString()
    ))
    set(value) {
        logUpdateDefaultTemplateId(value)
        prefStore.putString(FIRESTORE_PREF_DEFAULT_TEMPLATE_ID, value)
    }

@get:AppCompatDelegate.NightMode
val nightMode: Int
    get() = when (val mode = prefStore.getString(FIRESTORE_PREF_NIGHT_MODE, "auto")) {
        "auto" -> if (Build.VERSION.SDK_INT >= 29) {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        } else {
            AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
        }
        "yes" -> AppCompatDelegate.MODE_NIGHT_YES
        "no" -> AppCompatDelegate.MODE_NIGHT_NO
        else -> error("Unknown night mode value: $mode")
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

private val localPrefs: SharedPreferences by lazy {
    RobotScouter.getSharedPreferences(FIRESTORE_PREFS, Context.MODE_PRIVATE)
}

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
                val pref = prefs[newIndex]
                when (id) {
                    FIRESTORE_PREF_LOCK_TEMPLATES,
                    FIRESTORE_PREF_HAS_SHOWN_ADD_TEAM_TUTORIAL,
                    FIRESTORE_PREF_HAS_SHOWN_SIGN_IN_TUTORIAL,
                    FIRESTORE_PREF_SHOULD_SHOW_RATING_DIALOG
                    -> putBoolean(id, pref as Boolean)

                    FIRESTORE_PREF_DEFAULT_TEMPLATE_ID,
                    FIRESTORE_PREF_NIGHT_MODE,
                    FIRESTORE_PREF_UPLOAD_MEDIA_TO_TBA
                    -> {
                        val value = pref as String

                        hasDefaultTemplateChanged = id == FIRESTORE_PREF_DEFAULT_TEMPLATE_ID &&
                                defaultTemplateId != value

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
    GlobalScope.launch {
        val prefs = try {
            userPrefs.getInBatches()
        } catch (e: Exception) {
            logBreadcrumb("clearPrefs: " + userPrefs.path)
            throw InvocationMarker(e)
        }

        for (pref in prefs) {
            val ref = pref.reference
            ref.delete().logFailures("clearPrefs", pref)
        }

        localPrefs.edit { clear() }
    }
}

private fun updateTeamTemplateIds() {
    GlobalScope.launch {
        for (team in teams.waitForChange()) team.updateTemplateId(defaultTemplateId)
    }
}

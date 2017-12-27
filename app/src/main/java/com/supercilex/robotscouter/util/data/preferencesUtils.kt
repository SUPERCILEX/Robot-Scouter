package com.supercilex.robotscouter.util.data

import android.arch.lifecycle.DefaultLifecycleObserver
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.Observer
import android.content.Context
import android.content.SharedPreferences
import android.support.v7.app.AppCompatDelegate
import android.support.v7.preference.PreferenceDataStore
import com.firebase.ui.common.ChangeEventType
import com.firebase.ui.firestore.ObservableSnapshotArray
import com.google.firebase.firestore.DocumentSnapshot
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.data.model.TemplateType
import com.supercilex.robotscouter.util.FIRESTORE_PREFS
import com.supercilex.robotscouter.util.FIRESTORE_PREF_DEFAULT_TEMPLATE_ID
import com.supercilex.robotscouter.util.FIRESTORE_PREF_HAS_SHOWN_ADD_TEAM_TUTORIAL
import com.supercilex.robotscouter.util.FIRESTORE_PREF_HAS_SHOWN_SIGN_IN_TUTORIAL
import com.supercilex.robotscouter.util.FIRESTORE_PREF_NIGHT_MODE
import com.supercilex.robotscouter.util.FIRESTORE_PREF_SHOULD_SHOW_RATING_DIALOG
import com.supercilex.robotscouter.util.FIRESTORE_PREF_UPLOAD_MEDIA_TO_TBA
import com.supercilex.robotscouter.util.FIRESTORE_VALUE
import com.supercilex.robotscouter.util.async
import com.supercilex.robotscouter.util.data.model.updateTemplateId
import com.supercilex.robotscouter.util.data.model.userPrefs
import com.supercilex.robotscouter.util.logFailures
import com.supercilex.robotscouter.util.logUpdateDefaultTemplateId
import com.supercilex.robotscouter.util.showRatingDialog

private val localPrefs: SharedPreferences by lazy {
    RobotScouter.INSTANCE.getSharedPreferences(FIRESTORE_PREFS, Context.MODE_PRIVATE)
}

val prefs = object : PreferenceDataStore() {
    override fun putString(key: String, value: String?) {
        if (value != null) {
            userPrefs.document(key).set(mapOf(FIRESTORE_VALUE to value)).logFailures()
        }
    }

    override fun getString(key: String, defValue: String?): String? =
            localPrefs.getString(key, defValue)

    override fun putBoolean(key: String, value: Boolean) {
        userPrefs.document(key).set(mapOf(FIRESTORE_VALUE to value)).logFailures()
    }

    override fun getBoolean(key: String, defValue: Boolean): Boolean =
            localPrefs.getBoolean(key, defValue)
}

var defaultTemplateId: String
    get() =
        prefs.getString(FIRESTORE_PREF_DEFAULT_TEMPLATE_ID, TemplateType.DEFAULT.id.toString())!!
    set(value) {
        logUpdateDefaultTemplateId(value)
        prefs.putString(FIRESTORE_PREF_DEFAULT_TEMPLATE_ID, value)
    }

@get:AppCompatDelegate.NightMode
val nightMode: Int
    get() {
        val mode = prefs.getString(FIRESTORE_PREF_NIGHT_MODE, "auto")
        return when (mode) {
            "auto" -> AppCompatDelegate.MODE_NIGHT_AUTO
            "yes" -> AppCompatDelegate.MODE_NIGHT_YES
            "no" -> AppCompatDelegate.MODE_NIGHT_NO
            else -> throw IllegalStateException("Unknown night mode value: $mode")
        }
    }

val shouldAskToUploadMediaToTba: Boolean
    get() = prefs.getString(FIRESTORE_PREF_UPLOAD_MEDIA_TO_TBA, "ask") == "ask"

var shouldUploadMediaToTba: Boolean
    get() = prefs.getString(FIRESTORE_PREF_UPLOAD_MEDIA_TO_TBA, "ask") == "yes"
    set(value) = prefs.putString(FIRESTORE_PREF_UPLOAD_MEDIA_TO_TBA, if (value) "yes" else "no")

var hasShownAddTeamTutorial: Boolean
    get() = prefs.getBoolean(FIRESTORE_PREF_HAS_SHOWN_ADD_TEAM_TUTORIAL, false)
    set(value) = prefs.putBoolean(FIRESTORE_PREF_HAS_SHOWN_ADD_TEAM_TUTORIAL, value)

var hasShownSignInTutorial: Boolean
    get() = prefs.getBoolean(FIRESTORE_PREF_HAS_SHOWN_SIGN_IN_TUTORIAL, false)
    set(value) = prefs.putBoolean(FIRESTORE_PREF_HAS_SHOWN_SIGN_IN_TUTORIAL, value)

var shouldShowRatingDialog: Boolean
    get() = showRatingDialog && prefs.getBoolean(FIRESTORE_PREF_SHOULD_SHOW_RATING_DIALOG, true)
    set(value) = prefs.putBoolean(FIRESTORE_PREF_SHOULD_SHOW_RATING_DIALOG, value)

fun initPrefs() {
    PrefUpdater
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
            is Boolean -> prefs.putBoolean(key, false)
            is String -> prefs.putString(key, null)
            else -> throw IllegalStateException(
                    "Unknown value type: ${value?.let { it::class.java }}")
        }
    }
    clearLocalPrefs()
}

private fun clearLocalPrefs() = localPrefs.updatePrefs { clear() }

private fun updateTeamTemplateIds() {
    TeamsLiveData.observeOnDataChanged().observeOnce {
        async { for (team in it) team.updateTemplateId(defaultTemplateId) }.logFailures()
    }
}

private inline fun SharedPreferences.updatePrefs(
        transaction: SharedPreferences.Editor.() -> Unit
) = edit().run {
    transaction()
    apply()
}

private object PrefUpdater : Observer<ObservableSnapshotArray<Any>>,
        DefaultLifecycleObserver, ChangeEventListenerBase {
    init {
        PrefsLiveData.observeForever(this)
    }

    override fun onChanged(prefs: ObservableSnapshotArray<Any>?) {
        val lifecycle = ListenerRegistrationLifecycleOwner.lifecycle
        if (prefs == null) {
            lifecycle.removeObserver(this)
            onStop(ListenerRegistrationLifecycleOwner)
            clearLocalPrefs()
        } else {
            lifecycle.addObserver(this)
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        PrefsLiveData.value?.addChangeEventListener(this)
    }

    override fun onStop(owner: LifecycleOwner) {
        PrefsLiveData.value?.removeChangeEventListener(this)
    }

    override fun onChildChanged(
            type: ChangeEventType,
            snapshot: DocumentSnapshot,
            newIndex: Int,
            oldIndex: Int
    ) {
        val id = snapshot.id

        if (type == ChangeEventType.ADDED || type == ChangeEventType.CHANGED) {
            var hasDefaultTemplateChanged = false

            localPrefs.updatePrefs {
                when (id) {
                    FIRESTORE_PREF_HAS_SHOWN_ADD_TEAM_TUTORIAL,
                    FIRESTORE_PREF_HAS_SHOWN_SIGN_IN_TUTORIAL,
                    FIRESTORE_PREF_SHOULD_SHOW_RATING_DIALOG
                    -> putBoolean(id, PrefsLiveData.value!![newIndex] as Boolean)

                    FIRESTORE_PREF_DEFAULT_TEMPLATE_ID,
                    FIRESTORE_PREF_NIGHT_MODE,
                    FIRESTORE_PREF_UPLOAD_MEDIA_TO_TBA
                    -> {
                        val value = PrefsLiveData.value!![newIndex] as String

                        hasDefaultTemplateChanged = id == FIRESTORE_PREF_DEFAULT_TEMPLATE_ID
                                && defaultTemplateId != value

                        putString(id, value)
                    }
                }
            }

            if (hasDefaultTemplateChanged) updateTeamTemplateIds()
        } else if (type == ChangeEventType.REMOVED) {
            localPrefs.updatePrefs { remove(id) }
        }
    }
}

package com.supercilex.robotscouter.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences

const private val TEAM_LIST_ACTIVITY_PREF_NAME = "com.supercilex.robotscouter.ui.teamlist.TeamListActivity"
const private val EXPORT_PREF_NAME = "spreadsheet_export"
const private val UPLOAD_MEDIA_PREF_NAME = "upload_media"


const private val HAS_SHOWN_TUTORIAL = "has_shown_tutorial"
const private val HAS_SHOWN_ADD_TEAM_TUTORIAL = "${HAS_SHOWN_TUTORIAL}_fab"
const private val HAS_SHOWN_SIGN_IN_TUTORIAL = "${HAS_SHOWN_TUTORIAL}_sign_in"

const private val SHOULD_ASK_TO_UPLOAD_MEDIA = "should_ask_to_upload_media"
const private val SHOULD_UPLOAD_MEDIA = "should_upload_media_to_tba"

@SuppressLint("CommitPrefEdits")
private inline fun SharedPreferences.updatePrefs(transaction: SharedPreferences.Editor.() -> Unit) = edit().run {
    transaction()
    apply()
}

private fun getTeamListActivityBoolean(context: Context, key: String): Boolean =
        context.getSharedPreferences(TEAM_LIST_ACTIVITY_PREF_NAME, Context.MODE_PRIVATE)
                .getBoolean(key, false)

private fun setTeamListActivityBoolean(context: Context, key: String, value: Boolean) =
        context.getSharedPreferences(TEAM_LIST_ACTIVITY_PREF_NAME, Context.MODE_PRIVATE)
                .updatePrefs { putBoolean(key, value) }

fun hasShownAddTeamTutorial(context: Context): Boolean =
        getTeamListActivityBoolean(context, HAS_SHOWN_ADD_TEAM_TUTORIAL)

fun setHasShownAddTeamTutorial(context: Context, value: Boolean) =
        setTeamListActivityBoolean(context, HAS_SHOWN_ADD_TEAM_TUTORIAL, value)

fun hasShownSignInTutorial(context: Context): Boolean =
        getTeamListActivityBoolean(context, HAS_SHOWN_SIGN_IN_TUTORIAL)

fun setHasShownSignInTutorial(context: Context, value: Boolean) =
        setTeamListActivityBoolean(context, HAS_SHOWN_SIGN_IN_TUTORIAL, value)

fun shouldShowExportHint(context: Context): Boolean =
        context.getSharedPreferences(EXPORT_PREF_NAME, Context.MODE_PRIVATE)
                .getBoolean(EXPORT_PREF_NAME, true)

fun setShouldShowExportHint(context: Context, value: Boolean) =
        context.getSharedPreferences(EXPORT_PREF_NAME, Context.MODE_PRIVATE)
                .updatePrefs { putBoolean(EXPORT_PREF_NAME, value) }

/**
 * @return A pair of booleans where the first is whether or not an upload media to TBA
 * * confirmation dialog should be shown and the second is whether or not the media should be
 * * uploaded to TBA.
 */
fun shouldAskToUploadMediaToTba(context: Context): Pair<Boolean, Boolean> {
    val prefs = context.getSharedPreferences(UPLOAD_MEDIA_PREF_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(SHOULD_ASK_TO_UPLOAD_MEDIA, true) to
            prefs.getBoolean(SHOULD_UPLOAD_MEDIA, false)
}

fun setShouldAskToUploadMediaToTba(context: Context, value: Pair<Boolean, Boolean>) =
        context.getSharedPreferences(UPLOAD_MEDIA_PREF_NAME, Context.MODE_PRIVATE).updatePrefs {
            putBoolean(SHOULD_ASK_TO_UPLOAD_MEDIA, value.first)
            putBoolean(SHOULD_UPLOAD_MEDIA, value.second)
        }

package com.supercilex.robotscouter.util.data

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcel
import android.os.PersistableBundle
import android.support.annotation.RequiresApi
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.data.model.Team
import org.jetbrains.anko.bundleOf
import java.util.ArrayList

const val TEAM_KEY = "com.supercilex.robotscouter.data.util.Team"
const val TEAMS_KEY = "com.supercilex.robotscouter.data.util.Teams"
const val TAB_KEY = "tab_key"
const val SCOUT_ARGS_KEY = "scout_args"
const val KEY_ADD_SCOUT = "add_scout"
const val KEY_OVERRIDE_TEMPLATE_KEY = "override_template_key"

fun Parcel.readBooleanCompat() = getBooleanForInt(readInt())

fun Parcel.writeBooleanCompat(value: Boolean) = writeInt(getIntForBoolean(value))

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun PersistableBundle.getBooleanCompat(key: String) = getBooleanForInt(getInt(key))

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun PersistableBundle.putBooleanCompat(key: String, value: Boolean) =
        putInt(key, getIntForBoolean(value))

private fun getBooleanForInt(value: Int) = value == 1

private fun getIntForBoolean(value: Boolean) = if (value) 1 else 0

@Suppress("UNCHECKED_CAST") // Trust the client
fun <T> Parcel.readBundleAsMap(): Map<String, T> = readBundleAsMap { get(it) as T }

inline fun <T> Parcel.readBundleAsMap(parse: Bundle.(String) -> T): Map<String, T> =
        readBundle(RobotScouter::class.java.classLoader).let { bundleToMap(it, parse) }

@Suppress("UNCHECKED_CAST") // Trust the client
fun <T> Bundle.getBundleAsMap(key: String): Map<String, T> = getBundleAsMap(key) { get(it) as T }

inline fun <T> Bundle.getBundleAsMap(key: String, parse: Bundle.(String) -> T): Map<String, T> =
        getBundle(key).let { bundleToMap(it, parse) }

@Suppress("UNCHECKED_CAST") // Trust the client
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun <T> PersistableBundle.getBundleAsMap(key: String) = getBundleAsMap(key) { get(it) as T }

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
inline fun <T> PersistableBundle.getBundleAsMap(key: String,
                                                parse: PersistableBundle.(String) -> T) =
        getPersistableBundle(key).let { bundle ->
            bundle.keySet().associate { it to bundle.parse(it) } as Map<String, T>
        }

inline fun <T> bundleToMap(bundle: Bundle, parse: Bundle.(String) -> T) =
        bundle.keySet().associate { it to bundle.parse(it) }

fun Team.toBundle() = bundleOf(TEAM_KEY to this@toBundle)

fun Intent.putExtra(teams: List<Team>): Intent = putExtra(TEAMS_KEY, ArrayList(teams))

fun List<Team>.toBundle() = bundleOf(TEAMS_KEY to ArrayList(this@toBundle))

fun Bundle.getTeam(): Team = getParcelable(TEAM_KEY)

fun Intent.getTeamListExtra(): List<Team> = getParcelableArrayListExtra(TEAMS_KEY)

fun Bundle.getTeamList(): List<Team> = getParcelableArrayList(TEAMS_KEY)

fun getTabIdBundle(key: String?) = bundleOf(TAB_KEY to key)

fun getTabId(bundle: Bundle?): String? = bundle?.getString(TAB_KEY)

fun getScoutBundle(team: Team,
                   addScout: Boolean = false,
                   overrideTemplateId: String? = null,
                   scoutId: String? = null): Bundle {
    if (!addScout && overrideTemplateId != null) {
        throw IllegalArgumentException("Can't use an override id without adding a scout.")
    }

    return team.toBundle().apply {
        putBoolean(KEY_ADD_SCOUT, addScout)
        putString(KEY_OVERRIDE_TEMPLATE_KEY, overrideTemplateId)
        putAll(getTabIdBundle(scoutId))
    }
}

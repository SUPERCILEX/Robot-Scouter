package com.supercilex.robotscouter.util.data

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcel
import android.os.PersistableBundle
import android.support.annotation.RequiresApi
import com.supercilex.robotscouter.data.model.Team
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

fun Team.toBundle() = Bundle().apply { putParcelable(TEAM_KEY, this@toBundle) }

fun Intent.putExtra(teams: List<Team>): Intent = putExtra(TEAMS_KEY, ArrayList(teams))

fun List<Team>.toBundle() =
        Bundle().apply { putParcelableArrayList(TEAMS_KEY, ArrayList(this@toBundle)) }

fun Bundle.getTeam(): Team = getParcelable(TEAM_KEY)

fun Intent.getTeamListExtra(): List<Team> = getParcelableArrayListExtra(TEAMS_KEY)

fun Bundle.getTeamList(): List<Team> = getParcelableArrayList(TEAMS_KEY)

fun getTabKeyBundle(key: String?) = Bundle().apply { putString(TAB_KEY, key) }

fun getTabKey(bundle: Bundle): String? = bundle.getString(TAB_KEY)

fun getScoutBundle(team: Team,
                   addScout: Boolean = false,
                   overrideTemplateKey: String? = null,
                   scoutKey: String? = null): Bundle {
    if (!addScout && overrideTemplateKey != null) {
        throw IllegalArgumentException("Can't use an override key without adding a scout.")
    }

    return team.toBundle().apply {
        putBoolean(KEY_ADD_SCOUT, addScout)
        putString(KEY_OVERRIDE_TEMPLATE_KEY, overrideTemplateKey)
        putAll(getTabKeyBundle(scoutKey))
    }
}

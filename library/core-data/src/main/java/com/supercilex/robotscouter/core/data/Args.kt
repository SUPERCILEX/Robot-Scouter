package com.supercilex.robotscouter.core.data

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.support.annotation.RequiresApi
import androidx.core.os.bundleOf
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.supercilex.robotscouter.core.model.Team

const val TEAM_KEY = "com.supercilex.robotscouter.data.util.Team"
const val TEAMS_KEY = "com.supercilex.robotscouter.data.util.Teams"
const val REF_KEY = "com.supercilex.robotscouter.REF"
const val TAB_KEY = "tab_key"
const val SCOUT_ARGS_KEY = "scout_args"
const val KEY_ADD_SCOUT = "add_scout"
const val KEY_OVERRIDE_TEMPLATE_KEY = "override_template_key"

fun <T : CharSequence> T?.nullOrFull() = if (isNullOrBlank()) null else this

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun PersistableBundle.getBooleanCompat(key: String) = getInt(key) == 1

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun PersistableBundle.putBooleanCompat(key: String, value: Boolean) =
        putInt(key, if (value) 1 else 0)

@Suppress("UNCHECKED_CAST") // Trust the client
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun <T> PersistableBundle.getBundleAsMap(key: String) = getBundleAsMap(key) { get(it) as T }

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
inline fun <T> PersistableBundle.getBundleAsMap(
        key: String,
        parse: PersistableBundle.(String) -> T
) = getPersistableBundle(key).let { bundle ->
    bundle.keySet().associate { it to bundle.parse(it) } as Map<String, T>
}

fun Bundle.putRef(ref: DocumentReference) = putString(REF_KEY, ref.path)

fun Bundle.getRef() = FirebaseFirestore.getInstance().document(getString(REF_KEY))

fun Team.toBundle() = bundleOf(TEAM_KEY to this@toBundle)

fun Intent.putExtra(teams: List<Team>): Intent = putExtra(TEAMS_KEY, ArrayList(teams))

fun List<Team>.toBundle() = bundleOf(TEAMS_KEY to ArrayList(this@toBundle))

fun Bundle.getTeam(): Team = getParcelable(TEAM_KEY)

fun Intent.getTeamListExtra(): List<Team> = getParcelableArrayListExtra(TEAMS_KEY)

fun Bundle.getTeamList(): List<Team> = getParcelableArrayList(TEAMS_KEY)

fun getTabIdBundle(key: String?) = bundleOf(TAB_KEY to key)

fun getTabId(bundle: Bundle?): String? = bundle?.getString(TAB_KEY)

fun getScoutBundle(
        team: Team,
        addScout: Boolean = false,
        overrideTemplateId: String? = null,
        scoutId: String? = null
): Bundle {
    require(addScout || overrideTemplateId == null) {
        "Can't use an override id without adding a scout."
    }

    return team.toBundle().apply {
        putBoolean(KEY_ADD_SCOUT, addScout)
        putString(KEY_OVERRIDE_TEMPLATE_KEY, overrideTemplateId)
        putAll(getTabIdBundle(scoutId))
    }
}

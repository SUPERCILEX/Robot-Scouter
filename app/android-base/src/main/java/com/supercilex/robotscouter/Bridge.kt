package com.supercilex.robotscouter

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.annotation.Size
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.app.FragmentManager
import com.supercilex.robotscouter.core.model.Team
import com.supercilex.robotscouter.shared.PermissionRequestHandler
import java.lang.reflect.Modifier

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
annotation class Bridge

interface TeamListFragmentCompanion {
    fun getInstance(manager: FragmentManager): Fragment

    companion object {
        const val TAG = "TeamListFragment"

        private val instance =
                Class.forName("com.supercilex.robotscouter.feature.teams.TeamListFragment")
                        .get<TeamListFragmentCompanion>()

        operator fun invoke() = instance
    }
}

interface NewTeamDialogCompanion {
    fun show(manager: FragmentManager)

    companion object {
        private val instance =
                Class.forName("com.supercilex.robotscouter.feature.teams.NewTeamDialog")
                        .get<NewTeamDialogCompanion>()

        operator fun invoke() = instance
    }
}

interface AutoScoutFragmentCompanion {
    fun getInstance(manager: FragmentManager): Fragment

    companion object {
        const val TAG = "AutoScoutFragment"

        private val instance =
                Class.forName("com.supercilex.robotscouter.feature.autoscout.AutoScoutFragment")
                        .get<AutoScoutFragmentCompanion>()

        operator fun invoke() = instance
    }
}

interface TabletScoutListFragmentCompanion {
    fun newInstance(args: Bundle): Fragment

    companion object {
        const val TAG = "TabletScoutListFrag"

        private val instance =
                Class.forName("com.supercilex.robotscouter.feature.scouts.TabletScoutListFragment")
                        .get<TabletScoutListFragmentCompanion>()

        operator fun invoke() = instance
    }
}

interface TabletScoutListFragmentBridge {
    fun addScoutWithSelector()

    fun showTeamDetails()
}

interface ScoutListActivityCompanion {
    fun createIntent(args: Bundle): Intent

    companion object {
        private val instance =
                Class.forName("com.supercilex.robotscouter.feature.scouts.ScoutListActivity")
                        .get<ScoutListActivityCompanion>()

        operator fun invoke() = instance
    }
}

interface TemplateListActivityCompanion {
    fun createIntent(templateId: String? = null): Intent

    companion object {
        private val instance =
                Class.forName("com.supercilex.robotscouter.feature.templates.TemplateListActivity")
                        .get<TemplateListActivityCompanion>()

        operator fun invoke() = instance
    }
}

interface SettingsActivityCompanion {
    fun show(context: Context)

    companion object {
        private val instance =
                Class.forName("com.supercilex.robotscouter.feature.settings.SettingsActivity")
                        .get<SettingsActivityCompanion>()

        operator fun invoke() = instance
    }
}

interface ExportServiceCompanion {
    /** @return true if an export was attempted, false otherwise */
    fun exportAndShareSpreadSheet(
            activity: FragmentActivity,
            permHandler: PermissionRequestHandler,
            @Size(min = 1) mutableTeams: List<Team>
    ): Boolean

    companion object {
        private val instance =
                Class.forName("com.supercilex.robotscouter.feature.exports.ExportService")
                        .get<ExportServiceCompanion>()

        operator fun invoke() = instance
    }
}

interface SelectedTeamsRetriever {
    val selectedTeams: List<Team>
}

interface TeamExporter {
    fun export()
}

interface DrawerToggler {
    fun toggle(enabled: Boolean)
}

interface SignInResolver {
    fun showSignInResolution()
}

private inline fun <reified T> Class<*>.get() = getOrNull<T>()
        ?: throw ClassNotFoundException("Feature $name is not available")

private inline fun <reified T> Class<*>.getOrNull() = declaredFields
        .filter { Modifier.isStatic(it.modifiers) }
        .map { it.apply { isAccessible = true }.get(null) }
        .filterIsInstance<T>()
        .singleOrNull()

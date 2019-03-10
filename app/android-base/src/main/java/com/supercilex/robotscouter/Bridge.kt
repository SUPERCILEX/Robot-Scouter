package com.supercilex.robotscouter

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.annotation.Size
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.observe
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.android.play.core.splitinstall.SplitInstallException
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.SplitInstallSessionState
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener
import com.google.android.play.core.splitinstall.model.SplitInstallErrorCode
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.ValueSeeker
import com.supercilex.robotscouter.core._globalContext
import com.supercilex.robotscouter.core.await
import com.supercilex.robotscouter.core.data.SingleLiveEvent
import com.supercilex.robotscouter.core.fastAddOnSuccessListener
import com.supercilex.robotscouter.core.model.Team
import com.supercilex.robotscouter.core.ui.ActivityBase
import com.supercilex.robotscouter.shared.PermissionRequestHandler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.tasks.asTask
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.longToast
import org.jetbrains.anko.runOnUiThread
import org.jetbrains.anko.toast
import java.lang.reflect.Modifier
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import com.google.android.play.core.tasks.Task as PlayTask

private val moduleStatus = MutableLiveData<SplitInstallSessionState?>()

internal fun initBridges() {
    TeamListFragmentCompanion()
    IntegratedScoutListFragmentCompanion()
    AutoScoutFragmentCompanion()
    TemplateListFragmentCompanion()
    NewTeamDialogCompanion()
    ScoutListActivityCompanion()
    TrashActivityCompanion()
    SettingsActivityCompanion()
}

internal fun ActivityBase.handleModuleInstalls(
        progressBar: ProgressBar
) = moduleStatus.observe(this) { state ->
    if (state == null) {
        progressBar.isVisible = false
        return@observe
    }

    state.resolutionIntent()?.let {
        startIntentSender(it.intentSender, null, 0, 0, 0)
        moduleStatus.value = null
        return@observe
    }

    progressBar.apply {
        isIndeterminate = state.status() != SplitInstallSessionStatus.DOWNLOADING
        isVisible = state.status() != SplitInstallSessionStatus.INSTALLED &&
                state.status() != SplitInstallSessionStatus.FAILED &&
                state.status() != SplitInstallSessionStatus.CANCELED
        progress = state.bytesDownloaded().toInt()
        max = state.totalBytesToDownload().toInt()

        if (isVisible) bringToFront()
    }
}.let { Unit }

fun Context.home(vararg params: Pair<String, Any?>) = intentFor<HomeActivity>(*params)

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

interface Refreshable {
    fun refresh()
}

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
annotation class Bridge

interface BridgeCompanion

interface InstalledBridgeCompanion : BridgeCompanion

interface DownloadableBridgeCompanion : BridgeCompanion

interface BridgeFinderCompanion {
    val moduleName: String
}

interface TeamListFragmentCompanion : InstalledBridgeCompanion {
    fun getInstance(manager: FragmentManager): Fragment

    companion object : InstalledBridgeFinderCompanion<TeamListFragmentCompanion>() {
        const val TAG = "TeamListFragment"
        override val moduleName = "teams"

        override val instance =
                requireClass("com.supercilex.robotscouter.feature.teams.TeamListFragment")
                        .get<TeamListFragmentCompanion>()
    }
}

interface TeamSelectionListener {
    fun onTeamSelected(args: Bundle, transitionView: View? = null)
}

interface NewTeamDialogCompanion : InstalledBridgeCompanion {
    fun show(manager: FragmentManager)

    companion object : InstalledBridgeFinderCompanion<NewTeamDialogCompanion>() {
        override val moduleName = "teams"
        override val instance =
                requireClass("com.supercilex.robotscouter.feature.teams.NewTeamDialog")
                        .get<NewTeamDialogCompanion>()
    }
}

interface AutoScoutFragmentCompanion : InstalledBridgeCompanion {
    fun getInstance(manager: FragmentManager): Fragment

    companion object : InstalledBridgeFinderCompanion<AutoScoutFragmentCompanion>() {
        const val TAG = "AutoScoutFragment"
        override val moduleName = "autoscout"

        override val instance =
                requireClass("com.supercilex.robotscouter.feature.autoscout.AutoScoutFragment")
                        .get<AutoScoutFragmentCompanion>()
    }
}

interface ScoutListFragmentCompanionBase : InstalledBridgeCompanion {
    fun newInstance(args: Bundle): Fragment

    companion object {
        const val TAG = "ScoutListFragment"
    }
}

interface TabletScoutListFragmentCompanion : ScoutListFragmentCompanionBase {
    companion object : InstalledBridgeFinderCompanion<TabletScoutListFragmentCompanion>() {
        override val moduleName = "scouts"

        override val instance =
                requireClass("com.supercilex.robotscouter.feature.scouts.TabletScoutListContainer")
                        .get<TabletScoutListFragmentCompanion>()
    }
}

interface IntegratedScoutListFragmentCompanion : ScoutListFragmentCompanionBase {
    fun getInstance(manager: FragmentManager): Fragment?

    companion object : InstalledBridgeFinderCompanion<IntegratedScoutListFragmentCompanion>() {
        override val moduleName = "scouts"

        override val instance =
                requireClass("com.supercilex.robotscouter.feature.scouts.IntegratedScoutListFragment")
                        .get<IntegratedScoutListFragmentCompanion>()
    }
}

interface ScoutListActivityCompanion : InstalledBridgeCompanion {
    fun createIntent(args: Bundle): Intent

    companion object : InstalledBridgeFinderCompanion<ScoutListActivityCompanion>() {
        override val moduleName = "scouts"
        override val instance =
                requireClass("com.supercilex.robotscouter.feature.scouts.ScoutListActivity")
                        .get<ScoutListActivityCompanion>()
    }
}

interface TemplateListFragmentCompanion : InstalledBridgeCompanion {
    fun getInstance(manager: FragmentManager, args: Bundle? = null): Fragment

    companion object : InstalledBridgeFinderCompanion<TemplateListFragmentCompanion>() {
        const val TAG = "TemplateListFragment"
        override val moduleName = "templates"

        override val instance =
                requireClass("com.supercilex.robotscouter.feature.templates.TemplateListFragment")
                        .get<TemplateListFragmentCompanion>()
    }
}

interface TemplateListFragmentBridge {
    fun handleArgs(args: Bundle)
}

interface ExportServiceCompanion : DownloadableBridgeCompanion {
    /** @return true if an export was attempted, false otherwise */
    fun exportAndShareSpreadSheet(
            activity: FragmentActivity,
            permHandler: PermissionRequestHandler,
            @Size(min = 1) teams: List<Team>
    ): Boolean

    companion object : DownloadableBridgeFinderCompanion<ExportServiceCompanion>() {
        override val moduleName = "exports"
        override val instance by ValueSeeker {
            getClass("com.supercilex.robotscouter.feature.exports.ExportService")
                    ?.get<ExportServiceCompanion>()
        }
    }
}

interface TrashActivityCompanion : InstalledBridgeCompanion {
    fun createIntent(): Intent

    companion object : InstalledBridgeFinderCompanion<TrashActivityCompanion>() {
        override val moduleName = "trash"
        override val instance =
                requireClass("com.supercilex.robotscouter.feature.trash.TrashActivity")
                        .get<TrashActivityCompanion>()
    }
}

interface SettingsActivityCompanion : InstalledBridgeCompanion {
    fun createIntent(): Intent

    companion object : InstalledBridgeFinderCompanion<SettingsActivityCompanion>() {
        override val moduleName = "settings"
        override val instance =
                requireClass("com.supercilex.robotscouter.feature.settings.SettingsActivity")
                        .get<SettingsActivityCompanion>()
    }
}

abstract class InstalledBridgeFinderCompanion<T : InstalledBridgeCompanion>
    : BridgeFinderCompanion {
    abstract val instance: T

    operator fun invoke() = instance
}

abstract class DownloadableBridgeFinderCompanion<T : DownloadableBridgeCompanion>
    : BridgeFinderCompanion {
    abstract val instance: T?

    operator fun invoke(): Task<T> {
        return instance?.let { Tasks.forResult(it) } ?: GlobalScope.async {
            val existingRequests = manager.sessionStates.await().filter {
                it.moduleNames().contains(moduleName)
            }
            val isNewRequest = existingRequests.isEmpty()
            val sessionId = if (isNewRequest) {
                try {
                    manager.startInstall(SplitInstallRequest.newBuilder().addModule(moduleName).build())
                            .await()
                } catch (e: Exception) {
                    val isNetworkError = e is SplitInstallException &&
                            e.errorCode == SplitInstallErrorCode.NETWORK_ERROR
                    RobotScouter.runOnUiThread {
                        if (isNetworkError) toast(R.string.no_connection) else toast(R.string.error_unknown)
                    }
                    throw if (isNetworkError) CancellationException() else e
                }
            } else {
                existingRequests.first().sessionId()
            }

            RobotScouter.runOnUiThread { longToast(R.string.installing_module) }
            suspendCoroutine { c: Continuation<T> ->
                manager.registerListener(object : SplitInstallStateUpdatedListener {
                    override fun onStateUpdate(state: SplitInstallSessionState) {
                        if (state.sessionId() != sessionId) return

                        updateState(state)
                        when (state.status()) {
                            SplitInstallSessionStatus.INSTALLED -> success()

                            SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION,
                            SplitInstallSessionStatus.DOWNLOADING,
                            SplitInstallSessionStatus.PENDING,
                            SplitInstallSessionStatus.DOWNLOADED,
                            SplitInstallSessionStatus.INSTALLING,
                            SplitInstallSessionStatus.CANCELING
                            -> Unit // Wait for terminal state

                            else -> failure(Exception(state.toString()))
                        }
                    }

                    private fun success() {
                        cleanup()
                        val instance = instance
                        if (instance == null) {
                            RobotScouter.runOnUiThread { toast(R.string.error_unknown) }
                            c.resumeWithException(IllegalStateException(
                                    "Module should be installed, but classpath is unavailable."))
                        } else {
                            _globalContext =
                                    RobotScouter.createPackageContext(RobotScouter.packageName, 0)
                            c.resume(instance)
                        }
                    }

                    private fun failure(e: Exception) {
                        cleanup()
                        RobotScouter.runOnUiThread { toast(R.string.error_unknown) }
                        c.resumeWithException(e)
                    }

                    private fun cleanup() {
                        manager.unregisterListener(this)
                        updateState(null)
                    }

                    private fun updateState(state: SplitInstallSessionState?) {
                        if (isNewRequest) moduleStatus.postValue(state)
                    }
                })
            }
        }.asTask()
    }

    private suspend fun <T> PlayTask<T>.await() = asRealTask().await()

    private fun <T> PlayTask<T>.asRealTask() = TaskCompletionSource<T>().apply {
        addOnSuccessListener { setResult(it) }
        addOnFailureListener { setException(it) }
    }.task

    private companion object {
        val manager: SplitInstallManager = SplitInstallManagerFactory.create(RobotScouter)
    }
}

/**
 * We can't directly wait for the module install tasks to complete because the container activity
 * will be restarted on install. This class stores the result for retrieval on restart.
 */
internal class ModuleRequestHolder : ViewModel() {
    private val _onSuccess = SingleLiveEvent<Pair<DownloadableBridgeCompanion, List<Any?>>>()
    val onSuccess: LiveData<Pair<DownloadableBridgeCompanion, List<Any?>>> = _onSuccess

    operator fun plusAssign(request: Task<out DownloadableBridgeCompanion>) =
            plusAssign(request to emptyList())

    operator fun plusAssign(request: Pair<Task<out DownloadableBridgeCompanion>, List<Any?>>) {
        request.first.fastAddOnSuccessListener { _onSuccess.value = it to request.second }
    }
}

private fun requireClass(clazz: String) = try {
    Class.forName(clazz)
} catch (e: ClassNotFoundException) {
    throw IllegalStateException("Feature $clazz is unavailable", e)
}

private fun getClass(clazz: String) = try {
    Class.forName(clazz)
} catch (e: ClassNotFoundException) {
    null
}

private inline fun <reified T> Class<*>.get() = declaredFields
        .filter { Modifier.isStatic(it.modifiers) }
        .map { it.apply { isAccessible = true }.get(null) }
        .filterIsInstance<T>()
        .single()

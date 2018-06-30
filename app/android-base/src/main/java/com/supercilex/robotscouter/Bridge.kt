package com.supercilex.robotscouter

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import android.content.Intent
import android.os.Bundle
import android.support.annotation.Size
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.app.FragmentManager
import android.widget.ProgressBar
import androidx.core.view.isVisible
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
import com.supercilex.robotscouter.core.asTask
import com.supercilex.robotscouter.core.await
import com.supercilex.robotscouter.core.data.SingleLiveEvent
import com.supercilex.robotscouter.core.fastAddOnSuccessListener
import com.supercilex.robotscouter.core.model.Team
import com.supercilex.robotscouter.core.ui.ActivityBase
import com.supercilex.robotscouter.shared.PermissionRequestHandler
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.async
import org.jetbrains.anko.longToast
import org.jetbrains.anko.runOnUiThread
import org.jetbrains.anko.toast
import java.lang.reflect.Modifier
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine
import com.google.android.play.core.tasks.Task as PlayTask

private val moduleStatus = MutableLiveData<SplitInstallSessionState?>()

internal fun ActivityBase.handleModuleInstalls(
        progressBar: ProgressBar
) = moduleStatus.observe(this, Observer { state ->
    if (state == null) {
        progressBar.isVisible = false
        return@Observer
    }

    state.resolutionIntent()?.let {
        startIntentSender(it.intentSender, null, 0, 0, 0)
        moduleStatus.value = null
        return@Observer
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
})

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

        // TODO change to `requireClass`; https://issuetracker.google.com/issues/111017400
        override val instance =
                Class.forName("com.supercilex.robotscouter.feature.autoscout.AutoScoutFragment")
                        .get<AutoScoutFragmentCompanion>()
    }
}

interface TabletScoutListFragmentCompanion : InstalledBridgeCompanion {
    fun newInstance(args: Bundle): Fragment

    companion object : InstalledBridgeFinderCompanion<TabletScoutListFragmentCompanion>() {
        const val TAG = "TabletScoutListFrag"
        override val moduleName = "scouts"

        override val instance =
                requireClass("com.supercilex.robotscouter.feature.scouts.TabletScoutListFragment")
                        .get<TabletScoutListFragmentCompanion>()
    }
}

interface TabletScoutListFragmentBridge {
    fun addScoutWithSelector()

    fun showTeamDetails()
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

interface TemplateListActivityCompanion : DownloadableBridgeCompanion {
    fun createIntent(templateId: String? = null): Intent

    companion object : DownloadableBridgeFinderCompanion<TemplateListActivityCompanion>() {
        override val moduleName = "templates"
        override val instance by ValueSeeker {
            getClass("com.supercilex.robotscouter.feature.templates.TemplateListActivity")
                    ?.get<TemplateListActivityCompanion>()
        }
    }
}

interface SettingsActivityCompanion : DownloadableBridgeCompanion {
    fun createIntent(): Intent

    companion object : DownloadableBridgeFinderCompanion<SettingsActivityCompanion>() {
        override val moduleName = "settings"
        override val instance by ValueSeeker {
            getClass("com.supercilex.robotscouter.feature.settings.SettingsActivity")
                    ?.get<SettingsActivityCompanion>()
        }
    }
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

abstract class InstalledBridgeFinderCompanion<T : InstalledBridgeCompanion>
    : BridgeFinderCompanion {
    abstract val instance: T

    operator fun invoke() = instance
}

abstract class DownloadableBridgeFinderCompanion<T : DownloadableBridgeCompanion>
    : BridgeFinderCompanion {
    abstract val instance: T?

    operator fun invoke(): Task<T> {
        return instance?.let { Tasks.forResult(it) } ?: async {
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

private fun requireClass(clazz: String) = checkNotNull(getClass(clazz)) {
    "Feature $clazz is unavailable"
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

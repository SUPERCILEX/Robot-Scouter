package com.supercilex.robotscouter.feature.settings

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.bumptech.glide.Glide
import com.firebase.ui.auth.AuthUI
import com.google.firebase.appindexing.FirebaseAppIndex
import com.supercilex.robotscouter.core.CrashLogger
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.await
import com.supercilex.robotscouter.core.data.ViewModelBase
import com.supercilex.robotscouter.core.data.cancelAllAuthenticatedJobs
import com.supercilex.robotscouter.core.logFailures
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch

internal class SettingsViewModel : ViewModelBase<Unit?>() {
    private val _signOutListener = MutableLiveData<Any?>()
    val signOutListener: LiveData<Any?> = _signOutListener

    override fun onCreate(args: Unit?) = Unit

    fun signOut() {
        launch(UI) {
            try {
                async {
                    Glide.get(RobotScouter).clearDiskCache()
                    cancelAllAuthenticatedJobs()
                    FirebaseAppIndex.getInstance().removeAll().logFailures()
                    AuthUI.getInstance().signOut(RobotScouter).await()
                }.await()
                Glide.get(RobotScouter).clearMemory()

                _signOutListener.value = null
            } catch (e: Exception) {
                _signOutListener.value = e
                CrashLogger.onFailure(e)
            }
        }
    }
}

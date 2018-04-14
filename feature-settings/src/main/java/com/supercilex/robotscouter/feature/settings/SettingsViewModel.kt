package com.supercilex.robotscouter.feature.settings

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.firebase.ui.auth.AuthUI
import com.supercilex.robotscouter.core.CrashLogger
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.await
import com.supercilex.robotscouter.core.data.ViewModelBase
import com.supercilex.robotscouter.core.logFailures
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async

internal class SettingsViewModel : ViewModelBase<Any?>() {
    private val _signOutListener = MutableLiveData<Any?>()
    val signOutListener: LiveData<Any?> = _signOutListener

    override fun onCreate(args: Any?) = Unit

    fun signOut() {
        async(UI) {
            try {
                async { AuthUI.getInstance().signOut(RobotScouter).await() }.await()
                _signOutListener.value = null
            } catch (e: Exception) {
                _signOutListener.value = e
                CrashLogger.onFailure(e)
            }
        }.logFailures()
    }
}

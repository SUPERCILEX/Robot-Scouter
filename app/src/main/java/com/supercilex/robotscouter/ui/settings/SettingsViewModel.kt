package com.supercilex.robotscouter.ui.settings

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.firebase.ui.auth.AuthUI
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.util.CrashLogger
import com.supercilex.robotscouter.util.await
import com.supercilex.robotscouter.util.data.ViewModelBase
import com.supercilex.robotscouter.util.logFailures
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async

class SettingsViewModel : ViewModelBase<Any?>() {
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

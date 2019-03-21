package com.supercilex.robotscouter.feature.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.supercilex.robotscouter.core.CrashLogger
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.data.ViewModelBase
import com.supercilex.robotscouter.core.data.cleanup
import com.supercilex.robotscouter.shared.client.idpSignOut
import kotlinx.coroutines.launch

internal class SettingsViewModel : ViewModelBase<Unit?>() {
    private val _signOutListener = MutableLiveData<Any?>()
    val signOutListener: LiveData<Any?> = _signOutListener

    override fun onCreate(args: Unit?) = Unit

    fun signOut() {
        cleanup()
        Glide.get(RobotScouter).clearMemory()

        viewModelScope.launch {
            try {
                idpSignOut()
                _signOutListener.value = null
            } catch (e: Exception) {
                _signOutListener.value = e
                CrashLogger.onFailure(e)
            }
        }
    }
}

package com.supercilex.robotscouter.feature.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.supercilex.robotscouter.core.CrashLogger
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.data.SimpleViewModelBase
import com.supercilex.robotscouter.core.data.cleanup
import com.supercilex.robotscouter.shared.client.idpSignOut
import kotlinx.coroutines.launch

internal class SettingsViewModel(state: SavedStateHandle) : SimpleViewModelBase(state) {
    private val _signOutListener = MutableLiveData<Exception?>()
    val signOutListener: LiveData<Exception?> = _signOutListener

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

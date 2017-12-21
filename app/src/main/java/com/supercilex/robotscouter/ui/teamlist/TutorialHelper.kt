package com.supercilex.robotscouter.ui.teamlist

import android.arch.lifecycle.DefaultLifecycleObserver
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.Observer
import com.firebase.ui.firestore.ObservableSnapshotArray
import com.supercilex.robotscouter.util.FIRESTORE_PREF_HAS_SHOWN_ADD_TEAM_TUTORIAL
import com.supercilex.robotscouter.util.FIRESTORE_PREF_HAS_SHOWN_SIGN_IN_TUTORIAL
import com.supercilex.robotscouter.util.data.ChangeEventListenerBase
import com.supercilex.robotscouter.util.data.ListenerRegistrationLifecycleOwner
import com.supercilex.robotscouter.util.data.PrefsLiveData
import com.supercilex.robotscouter.util.data.TeamsLiveData
import com.supercilex.robotscouter.util.data.UniqueMutableLiveData
import com.supercilex.robotscouter.util.data.ViewModelBase
import com.supercilex.robotscouter.util.data.getPrefOrDefault

class TutorialHelper : ViewModelBase<Nothing?>(),
        DefaultLifecycleObserver, Observer<ObservableSnapshotArray<Any>>, ChangeEventListenerBase {
    val hasShownAddTeamTutorial = UniqueMutableLiveData<Boolean?>()
    val hasShownSignInTutorial = UniqueMutableLiveData<Boolean?>()

    override fun onCreate(args: Nothing?) = PrefsLiveData.observeForever(this)

    override fun onChanged(prefs: ObservableSnapshotArray<Any>?) {
        val lifecycle = ListenerRegistrationLifecycleOwner.lifecycle
        if (prefs == null) {
            lifecycle.removeObserver(this)
            onStop(ListenerRegistrationLifecycleOwner)
            hasShownAddTeamTutorial.setValue(null)
            hasShownSignInTutorial.setValue(null)
        } else {
            lifecycle.addObserver(this)
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        PrefsLiveData.value?.addChangeEventListener(this@TutorialHelper)
    }

    override fun onStop(owner: LifecycleOwner) {
        PrefsLiveData.value?.removeChangeEventListener(this@TutorialHelper)
    }

    override fun onDataChanged() {
        val prefs = PrefsLiveData.value!!
        hasShownAddTeamTutorial.setValue(
                prefs.getPrefOrDefault(FIRESTORE_PREF_HAS_SHOWN_ADD_TEAM_TUTORIAL, false))
        hasShownSignInTutorial.setValue(
                prefs.getPrefOrDefault(FIRESTORE_PREF_HAS_SHOWN_SIGN_IN_TUTORIAL, false)
                        && TeamsLiveData.value?.size?.let {
                    it >= MIN_TEAMS_TO_SHOW_SIGN_IN_TUTORIAL
                } == true
        )
    }

    override fun onCleared() {
        super.onCleared()
        PrefsLiveData.removeObserver(this)
    }

    private companion object {
        const val MIN_TEAMS_TO_SHOW_SIGN_IN_TUTORIAL = 3
    }
}

package com.supercilex.robotscouter.ui.teamlist

import android.arch.lifecycle.Observer
import com.firebase.ui.firestore.ObservableSnapshotArray
import com.supercilex.robotscouter.util.FIRESTORE_PREF_HAS_SHOWN_ADD_TEAM_TUTORIAL
import com.supercilex.robotscouter.util.FIRESTORE_PREF_HAS_SHOWN_SIGN_IN_TUTORIAL
import com.supercilex.robotscouter.util.data.ChangeEventListenerBase
import com.supercilex.robotscouter.util.data.PrefsLiveData
import com.supercilex.robotscouter.util.data.UniqueMutableLiveData
import com.supercilex.robotscouter.util.data.ViewModelBase
import com.supercilex.robotscouter.util.data.getPrefOrDefault

class TutorialHelper : ViewModelBase<Nothing?>(),
        Observer<ObservableSnapshotArray<Any>>, ChangeEventListenerBase {
    val hasShownAddTeamTutorial = UniqueMutableLiveData<Boolean?>()
    val hasShownSignInTutorial = UniqueMutableLiveData<Boolean?>()

    private var prefs: ObservableSnapshotArray<Any>? = null

    override fun onCreate(args: Nothing?) = PrefsLiveData.observeForever(this)

    override fun onChanged(prefs: ObservableSnapshotArray<Any>?) {
        if (prefs == null) {
            hasShownAddTeamTutorial.setValue(null)
            hasShownSignInTutorial.setValue(null)
        } else {
            this.prefs = prefs
            prefs.addChangeEventListener(this)
        }
    }

    override fun onDataChanged() {
        hasShownAddTeamTutorial.setValue(
                prefs!!.getPrefOrDefault(FIRESTORE_PREF_HAS_SHOWN_ADD_TEAM_TUTORIAL, false))
        hasShownSignInTutorial.setValue(
                prefs!!.getPrefOrDefault(FIRESTORE_PREF_HAS_SHOWN_SIGN_IN_TUTORIAL, false))
    }

    override fun onCleared() {
        super.onCleared()
        PrefsLiveData.removeObserver(this)
        prefs?.removeChangeEventListener(this)
    }
}

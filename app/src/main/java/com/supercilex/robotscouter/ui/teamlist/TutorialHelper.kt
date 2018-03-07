package com.supercilex.robotscouter.ui.teamlist

import com.supercilex.robotscouter.util.FIRESTORE_PREF_HAS_SHOWN_ADD_TEAM_TUTORIAL
import com.supercilex.robotscouter.util.FIRESTORE_PREF_HAS_SHOWN_SIGN_IN_TUTORIAL
import com.supercilex.robotscouter.util.data.ChangeEventListenerBase
import com.supercilex.robotscouter.util.data.UniqueMutableLiveData
import com.supercilex.robotscouter.util.data.ViewModelBase
import com.supercilex.robotscouter.util.data.getPrefOrDefault
import com.supercilex.robotscouter.util.data.prefs
import com.supercilex.robotscouter.util.data.teams

class TutorialHelper : ViewModelBase<Unit?>(), ChangeEventListenerBase {
    val hasShownAddTeamTutorial = UniqueMutableLiveData<Boolean?>()
    val hasShownSignInTutorial = UniqueMutableLiveData<Boolean?>()

    private val signInPrefUpdater = object : ChangeEventListenerBase {
        override fun onDataChanged() {
            hasShownSignInTutorial.setValue(
                    prefs.getPrefOrDefault(FIRESTORE_PREF_HAS_SHOWN_SIGN_IN_TUTORIAL, false)
                            || teams.size < MIN_TEAMS_TO_SHOW_SIGN_IN_TUTORIAL
            )
        }
    }

    override fun onCreate(args: Unit?) {
        prefs.keepAlive = true
        prefs.addChangeEventListener(this)
    }

    override fun onDataChanged() {
        teams.apply {
            removeChangeEventListener(signInPrefUpdater)
            addChangeEventListener(signInPrefUpdater)
        }
        hasShownAddTeamTutorial.setValue(prefs.getPrefOrDefault(
                FIRESTORE_PREF_HAS_SHOWN_ADD_TEAM_TUTORIAL, false))
    }

    override fun onCleared() {
        super.onCleared()
        prefs.removeChangeEventListener(this)
        prefs.keepAlive = false
    }

    private companion object {
        const val MIN_TEAMS_TO_SHOW_SIGN_IN_TUTORIAL = 3
    }
}

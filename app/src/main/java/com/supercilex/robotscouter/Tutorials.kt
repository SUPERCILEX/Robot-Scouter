package com.supercilex.robotscouter

import android.arch.lifecycle.Observer
import android.support.v4.app.FragmentActivity
import com.supercilex.robotscouter.common.FIRESTORE_PREF_HAS_SHOWN_ADD_TEAM_TUTORIAL
import com.supercilex.robotscouter.common.FIRESTORE_PREF_HAS_SHOWN_SIGN_IN_TUTORIAL
import com.supercilex.robotscouter.core.data.ChangeEventListenerBase
import com.supercilex.robotscouter.core.data.UniqueMutableLiveData
import com.supercilex.robotscouter.core.data.ViewModelBase
import com.supercilex.robotscouter.core.data.getPrefOrDefault
import com.supercilex.robotscouter.core.data.hasShownAddTeamTutorial
import com.supercilex.robotscouter.core.data.hasShownSignInTutorial
import com.supercilex.robotscouter.core.data.isSignedIn
import com.supercilex.robotscouter.core.data.mainHandler
import com.supercilex.robotscouter.core.data.prefs
import com.supercilex.robotscouter.core.data.teams
import org.jetbrains.anko.find
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt

fun showAddTeamTutorial(helper: TutorialHelper, owner: FragmentActivity) {
    helper.hasShownAddTeamTutorial.observe(owner, object : Observer<Boolean?> {
        private val prompt = MaterialTapTargetPrompt.Builder(
                owner, R.style.RobotScouter_Tutorial)
                .setTarget(R.id.fab)
                .setClipToView(owner.find(R.id.root))
                .setPrimaryText(R.string.tutorial_create_first_team_title)
                .setAutoDismiss(false)
                .setPromptStateChangeListener { _, state ->
                    if (state == MaterialTapTargetPrompt.STATE_FOCAL_PRESSED) {
                        hasShownAddTeamTutorial = true
                    }
                }
                .create()!!

        override fun onChanged(hasShownTutorial: Boolean?) {
            if (hasShownTutorial == false) prompt.show() else prompt.dismiss()
        }
    })
}

fun showSignInTutorial(
        helper: TutorialHelper,
        owner: FragmentActivity
) = mainHandler.post {
    helper.hasShownSignInTutorial.observe(owner, object : Observer<Boolean?> {
        private val prompt
            get() = MaterialTapTargetPrompt.Builder(owner, R.style.RobotScouter_Tutorial_Menu)
                    .setTarget(R.id.action_sign_in)
                    .setClipToView(owner.find(R.id.root))
                    .setPrimaryText(R.string.tutorial_sign_in_title)
                    .setSecondaryText(R.string.tutorial_sign_in_rationale)
                    .setPromptStateChangeListener { _, state ->
                        if (state == MaterialTapTargetPrompt.STATE_FOCAL_PRESSED
                                || state == MaterialTapTargetPrompt.STATE_NON_FOCAL_PRESSED) {
                            hasShownSignInTutorial = true
                        }
                    }
                    .create()
        private var latestPrompt: MaterialTapTargetPrompt? = null

        override fun onChanged(hasShownTutorial: Boolean?) {
            if (hasShownAddTeamTutorial && hasShownTutorial == false) prompt?.apply {
                show()
                latestPrompt = this
            } else latestPrompt?.dismiss()
        }
    })
}.let { Unit }

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
        if (!isSignedIn) return

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

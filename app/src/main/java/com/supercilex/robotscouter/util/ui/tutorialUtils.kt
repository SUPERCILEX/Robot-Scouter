package com.supercilex.robotscouter.util.ui

import android.arch.core.executor.ArchTaskExecutor
import android.arch.lifecycle.Observer
import android.support.v4.app.FragmentActivity
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.ui.teamlist.TutorialHelper
import com.supercilex.robotscouter.util.data.hasShownAddTeamTutorial
import com.supercilex.robotscouter.util.data.hasShownSignInTutorial
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
) = ArchTaskExecutor.getInstance().postToMainThread {
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

        override fun onChanged(hasShownTutorial: Boolean?) {
            if (hasShownAddTeamTutorial && hasShownTutorial == false) prompt?.show()
            else prompt?.dismiss()
        }
    })
}

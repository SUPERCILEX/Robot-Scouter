package com.supercilex.robotscouter.ui.teamlist

import android.app.Activity
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.util.data.hasShownAddTeamTutorial
import com.supercilex.robotscouter.util.data.hasShownSignInTutorial
import com.supercilex.robotscouter.util.data.setHasShownAddTeamTutorial
import com.supercilex.robotscouter.util.data.setHasShownSignInTutorial
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt

fun showCreateFirstTeamPrompt(activity: Activity): MaterialTapTargetPrompt? {
    val appContext = activity.applicationContext
    if (!hasShownAddTeamTutorial(appContext)) {
        return MaterialTapTargetPrompt.Builder(activity, R.style.RobotScouter_Tutorial)
                .setTarget(R.id.fab)
                .setPrimaryText(R.string.create_first_team)
                // TODO Fix https://github.com/sjwall/MaterialTapTargetPrompt/pull/81 before official release
                .setAutoDismiss(false)
                .setPromptStateChangeListener { _, state ->
                    if (state == MaterialTapTargetPrompt.STATE_FOCAL_PRESSED) {
                        setHasShownAddTeamTutorial(appContext, true)
                    }
                }
                .show()
    }
    return null
}

fun showSignInPrompt(activity: Activity) {
    val appContext = activity.applicationContext
    if (hasShownAddTeamTutorial(appContext) && !hasShownSignInTutorial(appContext)) {
        MaterialTapTargetPrompt.Builder(activity, R.style.RobotScouter_Tutorial_Menu)
                .setTarget(R.id.action_sign_in)
                .setPrimaryText(R.string.sign_in)
                .setSecondaryText(R.string.sign_in_rationale)
                .setPromptStateChangeListener { _, state ->
                    if (state == MaterialTapTargetPrompt.STATE_FOCAL_PRESSED) {
                        setHasShownSignInTutorial(appContext, true)
                    }
                }
                .show()
    }
}

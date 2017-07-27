package com.supercilex.robotscouter.ui.teamlist

import android.app.Activity
import android.view.MotionEvent
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.util.data.hasShownAddTeamTutorial
import com.supercilex.robotscouter.util.data.hasShownSignInTutorial
import com.supercilex.robotscouter.util.data.setHasShownAddTeamTutorial
import com.supercilex.robotscouter.util.data.setHasShownSignInTutorial
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt

private object EmptyOnHidePromptListener : MaterialTapTargetPrompt.OnHidePromptListener {
    override fun onHidePrompt(event: MotionEvent, tappedTarget: Boolean) {}

    override fun onHidePromptComplete() {}
}

fun showCreateFirstTeamPrompt(activity: Activity): MaterialTapTargetPrompt? {
    val appContext = activity.applicationContext
    if (!hasShownAddTeamTutorial(appContext)) {
        return MaterialTapTargetPrompt.Builder(activity, R.style.RobotScouter_Tutorial)
                .setTarget(R.id.fab)
                .setPrimaryText(R.string.create_first_team)
                .setAutoDismiss(false)
                .setOnHidePromptListener(object : MaterialTapTargetPrompt.OnHidePromptListener by EmptyOnHidePromptListener {
                    override fun onHidePrompt(event: MotionEvent, tappedTarget: Boolean) {
                        setHasShownAddTeamTutorial(appContext, tappedTarget)
                    }
                })
                .show()
    }
    return null
}

// TODO maybe make a BottomSheet that explains why the user should sign in
fun showSignInPrompt(activity: Activity) {
    val appContext = activity.applicationContext
    if (hasShownAddTeamTutorial(appContext) && !hasShownSignInTutorial(appContext)) {
        MaterialTapTargetPrompt.Builder(activity, R.style.RobotScouter_Tutorial_Menu)
                .setTarget(R.id.action_sign_in)
                .setPrimaryText(R.string.sign_in)
                .setOnHidePromptListener(object : MaterialTapTargetPrompt.OnHidePromptListener by EmptyOnHidePromptListener {
                    override fun onHidePrompt(event: MotionEvent, tappedTarget: Boolean) {
                        setHasShownSignInTutorial(appContext, tappedTarget)
                    }
                })
                .show()
    }
}

package com.supercilex.robotscouter.ui.teamlist;

import android.app.Activity;
import android.content.Context;
import android.view.MotionEvent;

import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.util.PreferencesHelper;

import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt;

public final class TutorialHelper {
    private TutorialHelper() {
        throw new AssertionError("No instance for you!");
    }

    public static void showCreateFirstTeamPrompt(Activity activity) {
        final Context appContext = activity.getApplicationContext();
        if (!PreferencesHelper.hasShownFabTutorial(appContext)) {
            new MaterialTapTargetPrompt.Builder(activity, R.style.RobotScouter_Tutorial)
                    .setTarget(R.id.fab)
                    .setPrimaryText(R.string.create_first_team)
                    .setAutoDismiss(false)
                    .setOnHidePromptListener(new MaterialTapTargetPrompt.OnHidePromptListener() {
                        @Override
                        public void onHidePrompt(MotionEvent event, boolean tappedTarget) {
                            PreferencesHelper.setHasShownFabTutorial(appContext, tappedTarget);
                        }

                        @Override
                        public void onHidePromptComplete() {
                            // Noop
                        }
                    })
                    .show();
        }
    }

    public static void showSignInPrompt(Activity activity) {
        final Context appContext = activity.getApplicationContext();
        if (PreferencesHelper.hasShownFabTutorial(appContext)
                && !PreferencesHelper.hasShownSignInTutorial(appContext)) {
            new MaterialTapTargetPrompt.Builder(activity, R.style.RobotScouter_Tutorial_Menu)
                    .setTarget(R.id.action_sign_in)
                    .setPrimaryText(R.string.sign_in)
                    .setOnHidePromptListener(new MaterialTapTargetPrompt.OnHidePromptListener() {
                        @Override
                        public void onHidePrompt(MotionEvent event, boolean tappedTarget) {
                            PreferencesHelper.setHasShownSignInTutorial(appContext, tappedTarget);
                        }

                        @Override
                        public void onHidePromptComplete() {
                            // Noop
                        }
                    })
                    .show();
        }
    }
}

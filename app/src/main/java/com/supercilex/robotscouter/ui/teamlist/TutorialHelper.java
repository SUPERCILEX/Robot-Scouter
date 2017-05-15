package com.supercilex.robotscouter.ui.teamlist;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.Nullable;
import android.view.MotionEvent;

import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.util.PreferencesUtils;

import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt;

public final class TutorialHelper {
    private TutorialHelper() {
        throw new AssertionError("No instance for you!");
    }

    @Nullable
    public static MaterialTapTargetPrompt showCreateFirstTeamPrompt(Activity activity) {
        final Context appContext = activity.getApplicationContext();
        if (!PreferencesUtils.hasShownAddTeamTutorial(appContext)) {
            return new MaterialTapTargetPrompt.Builder(activity, R.style.RobotScouter_Tutorial)
                    .setTarget(R.id.fab)
                    .setPrimaryText(R.string.create_first_team)
                    .setAutoDismiss(false)
                    .setOnHidePromptListener(new MaterialTapTargetPrompt.OnHidePromptListener() {
                        @Override
                        public void onHidePrompt(MotionEvent event, boolean tappedTarget) {
                            PreferencesUtils.setHasShownAddTeamTutorial(appContext, tappedTarget);
                        }

                        @Override
                        public void onHidePromptComplete() {
                            // Noop
                        }
                    })
                    .show();
        }
        return null;
    }

    public static void showSignInPrompt(Activity activity) {
        final Context appContext = activity.getApplicationContext();
        if (PreferencesUtils.hasShownAddTeamTutorial(appContext)
                && !PreferencesUtils.hasShownSignInTutorial(appContext)) {
            new MaterialTapTargetPrompt.Builder(activity, R.style.RobotScouter_Tutorial_Menu)
                    .setTarget(R.id.action_sign_in)
                    .setPrimaryText(R.string.sign_in)
                    .setOnHidePromptListener(new MaterialTapTargetPrompt.OnHidePromptListener() {
                        @Override
                        public void onHidePrompt(MotionEvent event, boolean tappedTarget) {
                            PreferencesUtils.setHasShownSignInTutorial(appContext, tappedTarget);
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

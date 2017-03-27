package com.supercilex.robotscouter.ui.teamlist;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.MotionEvent;

import com.supercilex.robotscouter.R;

import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt;

public final class TutorialHelper {
    private static final String HAS_SHOWN_TUTORIAL = "has_shown_tutorial";
    private static final String HAS_SHOWN_TUTORIAL_FAB = HAS_SHOWN_TUTORIAL + "_fab";
    private static final String HAS_SHOWN_TUTORIAL_SIGN_IN = HAS_SHOWN_TUTORIAL + "_sign_in";

    private TutorialHelper() {
        throw new AssertionError("No instance for you!");
    }

    public static void showCreateFirstTeamPrompt(Activity activity) {
        final SharedPreferences preferences = activity.getPreferences(Context.MODE_PRIVATE);
        if (!preferences.getBoolean(HAS_SHOWN_TUTORIAL_FAB, false)) {
            new MaterialTapTargetPrompt.Builder(activity, R.style.RobotScouter_Tutorial)
                    .setTarget(R.id.fab)
                    .setPrimaryText(R.string.create_first_team)
                    .setAutoDismiss(false)
                    .setOnHidePromptListener(new MaterialTapTargetPrompt.OnHidePromptListener() {
                        @Override
                        public void onHidePrompt(MotionEvent event, boolean tappedTarget) {
                            preferences.edit()
                                    .putBoolean(HAS_SHOWN_TUTORIAL_FAB, tappedTarget)
                                    .apply();
                        }

                        @Override
                        public void onHidePromptComplete() {
                        }
                    })
                    .show();
        }
    }

    public static void showSignInPrompt(Activity activity) {
        final SharedPreferences preferences = activity.getPreferences(Context.MODE_PRIVATE);
        if (preferences.getBoolean(HAS_SHOWN_TUTORIAL_FAB, false)
                && !preferences.getBoolean(HAS_SHOWN_TUTORIAL_SIGN_IN, false)) {
            new MaterialTapTargetPrompt.Builder(activity, R.style.RobotScouter_Tutorial_Menu)
                    .setTarget(R.id.action_sign_in)
                    .setPrimaryText(R.string.sign_in)
                    .setOnHidePromptListener(new MaterialTapTargetPrompt.OnHidePromptListener() {
                        @Override
                        public void onHidePrompt(MotionEvent event, boolean tappedTarget) {
                            preferences.edit()
                                    .putBoolean(HAS_SHOWN_TUTORIAL_SIGN_IN, true)
                                    .apply();
                        }

                        @Override
                        public void onHidePromptComplete() {
                        }
                    })
                    .show();
        }
    }
}

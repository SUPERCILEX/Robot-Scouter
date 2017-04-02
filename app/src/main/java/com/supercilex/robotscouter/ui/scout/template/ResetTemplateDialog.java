package com.supercilex.robotscouter.ui.scout.template;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.view.View;

import com.google.firebase.database.DataSnapshot;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.RobotScouter;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.data.util.TeamHelper;
import com.supercilex.robotscouter.data.util.UserHelper;
import com.supercilex.robotscouter.util.Constants;

public class ResetTemplateDialog extends DialogFragment implements DialogInterface.OnShowListener, View.OnClickListener {
    private static final String TAG = "ResetTemplateDialog";
    private static final String RESET_ALL_KEY = "reset_all_key";

    public static void show(FragmentManager manager, TeamHelper helper, boolean shouldResetAll) {
        DialogFragment dialog = new ResetTemplateDialog();

        Bundle bundle = helper.toBundle();
        bundle.putBoolean(RESET_ALL_KEY, shouldResetAll);
        dialog.setArguments(bundle);

        dialog.show(manager, TAG);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(R.string.confirm_action)
                .setPositiveButton(R.string.reset, null)
                .setNegativeButton(android.R.string.no, null)
                .create();
        dialog.setOnShowListener(this);
        return dialog;
    }

    @Override
    public void onShow(DialogInterface dialog) {
        ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(this);
    }

    @Override
    public void dismiss() {
        super.dismiss();
        ((DialogFragment) getParentFragment()).dismiss();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        RobotScouter.getRefWatcher(getActivity()).watch(this);
    }

    @Override
    public void onClick(View v) {
        Bundle args = getArguments();
        final Team team = TeamHelper.get(getArguments()).getTeam();
        final String templateKey = team.getTemplateKey();

        if (args.getBoolean(RESET_ALL_KEY)) {
            for (DataSnapshot snapshot : Constants.sFirebaseTeams) {
                DataSnapshot keySnapshot = snapshot.child(Constants.FIREBASE_TEMPLATE_KEY);
                if (TextUtils.equals(templateKey, keySnapshot.getValue(String.class))) {
                    keySnapshot.getRef().removeValue();
                }
            }
            deleteTemplate(templateKey);
        } else {
            team.getHelper().getRef().child(Constants.FIREBASE_TEMPLATE_KEY).removeValue();
        }

        dismiss();
    }

    private void deleteTemplate(String key) {
        UserHelper.getScoutTemplateIndicesRef().child(key).removeValue();
        Constants.FIREBASE_SCOUT_TEMPLATES.child(key).removeValue();
    }
}

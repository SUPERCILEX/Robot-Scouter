package com.supercilex.robotscouter.ui.scout.template;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.view.View;

import com.firebase.ui.database.ChangeEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.data.util.TeamHelper;
import com.supercilex.robotscouter.util.Constants;

public class ResetTemplateSheet extends DialogFragment implements DialogInterface.OnShowListener, View.OnClickListener {
    private static final String TAG = "ResetTemplateSheet";
    private static final String RESET_ALL_KEY = "reset_all_key";

    public static void show(FragmentManager manager, TeamHelper helper, boolean shouldResetAll) {
        ResetTemplateSheet dialog = new ResetTemplateSheet();

        Bundle bundle = helper.getBundle();
        bundle.putBoolean(RESET_ALL_KEY, shouldResetAll);
        dialog.setArguments(bundle);

        dialog.show(manager, TAG);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(R.string.confirm_deletion)
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
    public void onClick(View v) {
        Bundle args = getArguments();
        Team team = TeamHelper.get(getArguments()).getTeam();
        final String templateKey = team.getTemplateKey();

        if (args.getBoolean(RESET_ALL_KEY)) {
            for (DataSnapshot snapshot : Constants.sFirebaseTeams) {
                DataSnapshot keySnapshot = snapshot.child(Constants.FIREBASE_TEMPLATE_KEY);
                if (TextUtils.equals(templateKey, keySnapshot.getValue(String.class))) {
                    keySnapshot.getRef().removeValue();
                }
            }
            Constants.FIREBASE_SCOUT_TEMPLATES.child(templateKey).removeValue();
            clearStoredTemplateKey();
            dismiss();
        } else {
            Constants.sFirebaseTeams.addChangeEventListener(new ChangeEventListener() {
                @Override
                public void onChildChanged(EventType type, int index, int oldIndex) {
                    DataSnapshot team1 = Constants.sFirebaseTeams.get(index);
                    if (TextUtils.equals(team1.getKey(), team1.getKey())
                            && team1.child(Constants.FIREBASE_TEMPLATE_KEY).getValue() == null) {
                        boolean isTemplateInUse = false;
                        for (DataSnapshot snapshot : Constants.sFirebaseTeams) {
                            String templateKey1 =
                                    snapshot.child(Constants.FIREBASE_TEMPLATE_KEY)
                                            .getValue(String.class);
                            if (TextUtils.equals(templateKey, templateKey1)) {
                                isTemplateInUse = true;
                                break;
                            }
                        }
                        if (!isTemplateInUse) {
                            Constants.FIREBASE_SCOUT_TEMPLATES.child(templateKey).removeValue();

                            String storedTemplateKey =
                                    getContext().getSharedPreferences(Constants.SCOUT_TEMPLATE,
                                                                      Context.MODE_PRIVATE)
                                            .getString(Constants.SCOUT_TEMPLATE, null);
                            if (TextUtils.equals(templateKey, storedTemplateKey)) { // NOPMD
                                clearStoredTemplateKey();
                            }
                        }

                        Constants.sFirebaseTeams.removeChangeEventListener(this);
                        dismiss();
                    }
                }

                @Override
                public void onDataChanged() {
                }

                @Override
                public void onCancelled(DatabaseError error) {
                }
            });

            team.getHelper().getRef().child(Constants.FIREBASE_TEMPLATE_KEY).removeValue();
        }
    }

    private void clearStoredTemplateKey() {
        getContext().getSharedPreferences(Constants.SCOUT_TEMPLATE, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
    }
}

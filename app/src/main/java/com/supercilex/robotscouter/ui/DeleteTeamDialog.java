package com.supercilex.robotscouter.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;

import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.Team;

public class DeleteTeamDialog extends DialogBase implements AlertDialog.OnClickListener {
    private static final String TAG = "DeleteTeamDialog";

    public static void show(FragmentManager manager, Team team) {
        DeleteTeamDialog dialog = new DeleteTeamDialog();
        dialog.setArguments(team.getBundle());
        dialog.show(manager, TAG);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getContext())
                .setTitle(R.string.confirm_deletion)
                .setPositiveButton(R.string.delete, this)
                .setNegativeButton(android.R.string.no, null)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        Team.getTeam(getArguments()).delete();
    }
}

package com.supercilex.robotscouter.ui.scout;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;

import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.util.TeamHelper;

public class ShouldDeleteTeamDialog extends DialogFragment implements Dialog.OnClickListener {
    private static final String TAG = "ShouldDeleteTeamDialog";
    private TeamHelper mTeamHelper;

    public static void show(FragmentManager manager, TeamHelper teamHelper) {
        DialogFragment dialog = new ShouldDeleteTeamDialog();
        dialog.setArguments(teamHelper.toBundle());
        dialog.show(manager, TAG);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTeamHelper = TeamHelper.parse(getArguments());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getContext())
                .setTitle(R.string.should_delete_team)
                .setMessage(getString(R.string.should_delete_team_message, mTeamHelper.toString()))
                .setPositiveButton(R.string.delete, this)
                .setNegativeButton(android.R.string.no, null)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        mTeamHelper.deleteTeam();
    }
}

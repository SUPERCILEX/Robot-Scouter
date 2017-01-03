package com.supercilex.robotscouter.ui.common;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;

import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.ui.DialogBase;

import java.util.List;

public class DeleteTeamDialog extends DialogBase implements AlertDialog.OnClickListener {
    private static final String TAG = "DeleteTeamDialog";
    private static final String TEAMS_KEY = "teams_key";

    public static void show(FragmentManager manager, List<Team> teams) {
        DeleteTeamDialog dialog = new DeleteTeamDialog();
        Bundle bundle = new Bundle();
        bundle.putParcelableArray(TEAMS_KEY, teams.toArray(new Team[teams.size()]));
        dialog.setArguments(bundle);
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
        for (Team team : (Team[]) getArguments().getParcelableArray(TEAMS_KEY)) {
            team.delete();
        }
    }
}

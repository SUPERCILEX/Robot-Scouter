package com.supercilex.robotscouter.ui.teamlist;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;

import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.util.TeamHelper;
import com.supercilex.robotscouter.util.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DeleteTeamDialog extends DialogFragment implements AlertDialog.OnClickListener {
    private static final String TAG = "DeleteTeamDialog";
    private static final String TEAMS_KEY = "teams_key";

    private List<TeamHelper> mTeamHelpers = new ArrayList<>();

    public static void show(FragmentManager manager, List<TeamHelper> teams) {
        DialogFragment dialog = new DeleteTeamDialog();

        Bundle args = new Bundle();
        args.putParcelableArray(TEAMS_KEY, teams.toArray(new TeamHelper[teams.size()]));
        dialog.setArguments(args);

        dialog.show(manager, TAG);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        for (Parcelable parcelable : getArguments().getParcelableArray(TEAMS_KEY)) {
            mTeamHelpers.add((TeamHelper) parcelable);
        }
        Collections.sort(mTeamHelpers);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        StringBuilder deletedTeams = new StringBuilder();
        for (int i = 0; i < mTeamHelpers.size(); i++) {
            deletedTeams.append(i + 1)
                    .append(". ")
                    .append(mTeamHelpers.get(i))
                    .append('\n');
        }

        return new AlertDialog.Builder(getContext())
                .setTitle(R.string.confirm_action)
                .setMessage(mTeamHelpers.size() == Constants.SINGLE_ITEM
                                    ? null : getString(R.string.caution_delete, deletedTeams))
                .setPositiveButton(R.string.delete, this)
                .setNegativeButton(android.R.string.no, null)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        for (TeamHelper teamHelper : mTeamHelpers) {
            teamHelper.deleteTeam();
        }
    }
}

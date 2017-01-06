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
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.util.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DeleteTeamDialog extends DialogFragment implements AlertDialog.OnClickListener {
    private static final String TAG = "DeleteTeamDialog";
    private static final String TEAMS_KEY = "teams_key";

    private List<Team> mTeams = new ArrayList<>();

    public static void show(FragmentManager manager, List<Team> teams) {
        DeleteTeamDialog dialog = new DeleteTeamDialog();
        Bundle bundle = new Bundle();
        bundle.putParcelableArray(TEAMS_KEY, teams.toArray(new Team[teams.size()]));
        dialog.setArguments(bundle);
        dialog.show(manager, TAG);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        for (Parcelable parcelable : getArguments().getParcelableArray(TEAMS_KEY)) {
            mTeams.add((Team) parcelable);
        }
        Collections.sort(mTeams);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        StringBuilder deletedTeams = new StringBuilder();
        for (int i = 0; i < mTeams.size(); i++) {
            deletedTeams.append(i + 1)
                    .append(". ")
                    .append(mTeams.get(i).getFormattedName())
                    .append('\n');
        }

        return new AlertDialog.Builder(getContext())
                .setTitle(R.string.confirm_deletion)
                .setMessage(mTeams.size() == Constants.SINGLE_ITEM
                                    ? null : getString(R.string.caution_delete, deletedTeams))
                .setPositiveButton(R.string.delete, this)
                .setNegativeButton(android.R.string.no, null)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        for (Team team : mTeams) {
            team.delete();
        }
    }
}

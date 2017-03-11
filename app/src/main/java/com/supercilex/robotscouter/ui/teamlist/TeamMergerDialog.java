package com.supercilex.robotscouter.ui.teamlist;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.data.util.FirebaseCopier;
import com.supercilex.robotscouter.data.util.ScoutUtils;
import com.supercilex.robotscouter.data.util.TeamHelper;
import com.supercilex.robotscouter.util.DatabaseHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TeamMergerDialog extends DialogFragment implements AlertDialog.OnClickListener, OnSuccessListener<DatabaseReference> {
    private static final String TAG = "TeamMergerDialog";
    private static final String TEAMS_KEY = "teams_key";

    private List<TeamHelper> mTeamHelpers = new ArrayList<>();

    public static void show(FragmentManager manager, List<TeamHelper> teamHelpers) {
        DialogFragment dialog = new TeamMergerDialog();

        Bundle args = new Bundle();
        args.putParcelableArray(TEAMS_KEY, teamHelpers.toArray(new TeamHelper[teamHelpers.size()]));
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
        return new AlertDialog.Builder(getContext())
                .setTitle(R.string.confirm_action)
                .setMessage(getString(R.string.merge_teams_warning, mTeamHelpers.get(0).toString()))
                .setPositiveButton(R.string.merge_teams, this)
                .setNegativeButton(android.R.string.no, null)
                .create();
    }

    @Override
    public void onClick(final DialogInterface dialog, int which) {
        final Team newTeam = mTeamHelpers.remove(mTeamHelpers.size() - 1).getTeam();
        final List<Task<Void>> deletionTasks = new ArrayList<Task<Void>>() {
            @Override
            public boolean add(Task<Void> task) {
                boolean result = super.add(task);
                if (size() == mTeamHelpers.size()) {
                    Tasks.whenAll(this).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            // If we are being called from TeamListFragment, reset the menu if the click was consumed
                            Fragment fragment = getParentFragment();
                            if (fragment instanceof TeamListFragment) {
                                ((TeamListFragment) fragment).resetMenu();
                            }
                            dismiss();
                        }
                    });
                }
                return result;
            }
        };

        for (TeamHelper teamHelper : mTeamHelpers) {
            final Team oldTeam = teamHelper.getTeam();

            DatabaseHelper.forceUpdate(ScoutUtils.getIndicesRef(oldTeam.getKey()))
                    .addOnSuccessListener(new OnSuccessListener<Query>() {
                        @Override
                        public void onSuccess(Query query) {
                            Task<List<Task<DatabaseReference>>> copyTask =
                                    new FirebaseCopier(query,
                                                       ScoutUtils.getIndicesRef(newTeam.getKey()))
                                            .performTransformation();
                            copyTask.addOnSuccessListener(new OnSuccessListener<List<Task<DatabaseReference>>>() {
                                @Override
                                public void onSuccess(List<Task<DatabaseReference>> tasks) {
                                    for (Task<DatabaseReference> task : tasks) {
                                        task.addOnSuccessListener(TeamMergerDialog.this);
                                    }

                                    deletionTasks.add(
                                            Tasks.whenAll(tasks)
                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            oldTeam.getHelper().deleteTeam();
                                                        }
                                                    }));
                                }
                            });
                        }
                    });
        }
    }


    @Override
    public void onSuccess(DatabaseReference ref) {
        ref.removeValue();
    }
}

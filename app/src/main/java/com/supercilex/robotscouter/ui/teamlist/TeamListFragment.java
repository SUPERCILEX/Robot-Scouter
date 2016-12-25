package com.supercilex.robotscouter.ui.teamlist;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.firebase.ui.database.FirebaseIndexRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.RobotScouter;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.ui.StickyFragment;
import com.supercilex.robotscouter.util.BaseHelper;
import com.supercilex.robotscouter.util.Constants;

import java.lang.ref.WeakReference;

public class TeamListFragment extends StickyFragment {
    private RecyclerView mTeams;
    private FirebaseRecyclerAdapter mAdapter;
    private WeakReference<LinearLayoutManager> mManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (AuthHelper.isSignedIn()) initAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.recycler_view, container, false);
        mTeams = (RecyclerView) rootView.findViewById(R.id.list);
        mTeams.setHasFixedSize(true);
        mManager = new WeakReference<>(new LinearLayoutManager(getContext()));
        mTeams.setLayoutManager(mManager.get());
        mTeams.setAdapter(mAdapter);
        BaseHelper.restoreRecyclerViewState(savedInstanceState, mAdapter, mManager.get());
        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mTeams.setAdapter(null);
        mTeams.setLayoutManager(null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cleanup();
        RobotScouter.getRefWatcher(getActivity()).watch(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        BaseHelper.saveRecyclerViewState(outState, mAdapter, mManager.get());
        super.onSaveInstanceState(outState);
    }

    public void cleanup() {
        mTeams.setAdapter(null);
        if (mAdapter != null) mAdapter.cleanup();
    }

    private void initAdapter() {
        mAdapter = new FirebaseIndexRecyclerAdapter<Team, TeamHolder>(
                Team.class,
                R.layout.team_list_row_layout,
                TeamHolder.class,
                Team.getIndicesRef(),
                Constants.FIREBASE_TEAMS) {
            @Override
            public void populateViewHolder(TeamHolder teamHolder, Team team, int position) {
                team.setKey(getRef(position).getKey());
                teamHolder.bind(TeamListFragment.this, team);
                team.fetchLatestData();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                FirebaseCrash.report(error.toException());
            }

            @Override
            public void onJoinFailed(int index, DataSnapshot snapshot) {
                super.onJoinFailed(index, snapshot);
                FirebaseCrash.report(new IllegalStateException("Index mismatch at index: "
                                                                       + index + " for snapshot: "
                                                                       + snapshot));
            }
        };
    }

    public void resetAdapter() {
        cleanup();
        initAdapter();
        mTeams.setAdapter(mAdapter);
    }
}

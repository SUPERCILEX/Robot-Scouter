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
import com.google.firebase.database.DatabaseError;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.RobotScouter;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.ui.StickyFragment;
import com.supercilex.robotscouter.util.BaseHelper;
import com.supercilex.robotscouter.util.Constants;

public class TeamsFragment extends StickyFragment {
    private RecyclerView mTeams;
    private FirebaseRecyclerAdapter mAdapter;
    private LinearLayoutManager mManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (BaseHelper.isSignedIn()) {
            initAdapter();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.recycler_view, container, false);

        mManager = new LinearLayoutManager(getContext());
        mTeams = (RecyclerView) rootView.findViewById(R.id.list);
        mTeams.setHasFixedSize(true);
        mTeams.setLayoutManager(mManager);
        mTeams.setAdapter(mAdapter);
        BaseHelper.restoreFirebaseRecyclerViewState(savedInstanceState, mAdapter, mManager);
        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        BaseHelper.saveFirebaseRecyclerViewState(outState, mAdapter, mManager);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mManager = null;
        mTeams.setAdapter(null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cleanup();
        RobotScouter.getRefWatcher(getActivity()).watch(this);
    }

    public void cleanup() {
        mTeams.setAdapter(null);
        if (mAdapter != null) {
            mAdapter.cleanup();
        }
    }

    private void initAdapter() {
        mAdapter = new FirebaseIndexRecyclerAdapter<Team, TeamHolder>(
                Team.class,
                R.layout.team_list_row_layout,
                TeamHolder.class,
                Team.getIndicesRef(),
                BaseHelper.getDatabase().child(Constants.FIREBASE_TEAMS)) {
            @Override
            public void populateViewHolder(TeamHolder teamHolder, Team team, int position) {
                team.setKey(getRef(position).getKey());
                teamHolder.setContext(getContext())
                        .setTeam(team)
                        .init();
                team.fetchLatestData();
            }

            @Override
            protected void onCancelled(DatabaseError databaseError) {
                FirebaseCrash.report(databaseError.toException());
            }
        };
    }

    public void setAdapter() {
        initAdapter();
        mTeams.setAdapter(mAdapter);
    }
}

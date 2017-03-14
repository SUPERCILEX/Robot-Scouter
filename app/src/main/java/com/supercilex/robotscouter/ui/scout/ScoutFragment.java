package com.supercilex.robotscouter.ui.scout;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.RobotScouter;
import com.supercilex.robotscouter.data.model.metrics.ScoutMetric;
import com.supercilex.robotscouter.data.util.ScoutUtils;
import com.supercilex.robotscouter.ui.scout.viewholder.ScoutViewHolderBase;
import com.supercilex.robotscouter.util.Constants;
import com.supercilex.robotscouter.util.FirebaseAdapterHelper;

public class ScoutFragment extends Fragment implements MenuItem.OnMenuItemClickListener {
    private static final String TEAM_KEY = "team_key";

    private FirebaseRecyclerAdapter<ScoutMetric, ScoutViewHolderBase> mAdapter;
    private RecyclerView mRecyclerView;
    private RecyclerView.LayoutManager mManager;
    private String mScoutKey;

    public static ScoutFragment newInstance(String teamKey, String scoutKey) {
        ScoutFragment fragment = new ScoutFragment();

        Bundle args = ScoutUtils.getScoutKeyBundle(scoutKey);
        args.putString(TEAM_KEY, teamKey);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mScoutKey = ScoutUtils.getScoutKey(getArguments());
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        mRecyclerView = (RecyclerView) inflater.inflate(R.layout.recycler_view, container, false);

        mManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mManager);
        mRecyclerView.setHasFixedSize(true);

        mAdapter = new ScoutAdapter(
                Constants.getScoutMetrics(mScoutKey),
                getChildFragmentManager(),
                (SimpleItemAnimator) mRecyclerView.getItemAnimator());
        mRecyclerView.setAdapter(mAdapter);
        FirebaseAdapterHelper.restoreRecyclerViewState(savedInstanceState, mAdapter, mManager);

        return mRecyclerView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        FirebaseAdapterHelper.saveRecyclerViewState(outState, mAdapter, mManager);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        super.onPause();
        mRecyclerView.clearFocus();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mAdapter.cleanup();
        RobotScouter.getRefWatcher(getActivity()).watch(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(Menu.NONE, R.id.action_delete, 100, R.string.delete_scout)
                .setOnMenuItemClickListener(this);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.action_delete) {
            ScoutUtils.delete(getArguments().getString(TEAM_KEY), mScoutKey);
            return true;
        }
        return false;
    }
}

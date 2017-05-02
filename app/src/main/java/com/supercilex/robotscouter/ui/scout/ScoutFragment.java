package com.supercilex.robotscouter.ui.scout;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.RobotScouter;
import com.supercilex.robotscouter.data.model.metrics.ScoutMetric;
import com.supercilex.robotscouter.data.util.ScoutUtils;
import com.supercilex.robotscouter.ui.scout.viewholder.ScoutViewHolderBase;
import com.supercilex.robotscouter.util.Constants;
import com.supercilex.robotscouter.util.FirebaseAdapterUtils;

public class ScoutFragment extends Fragment {
    private FirebaseRecyclerAdapter<ScoutMetric, ScoutViewHolderBase> mAdapter;
    private RecyclerView mRecyclerView;
    private RecyclerView.LayoutManager mManager;
    private String mScoutKey;

    public static ScoutFragment newInstance(String scoutKey) {
        ScoutFragment fragment = new ScoutFragment();
        fragment.setArguments(ScoutUtils.getScoutKeyBundle(scoutKey));
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
                mRecyclerView);
        mRecyclerView.setAdapter(mAdapter);
        FirebaseAdapterUtils.restoreRecyclerViewState(savedInstanceState, mAdapter, mManager);

        return mRecyclerView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        FirebaseAdapterUtils.saveRecyclerViewState(outState, mAdapter, mManager);
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
}

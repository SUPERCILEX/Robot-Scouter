package com.supercilex.robotscouter.ui.scout;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.RobotScouter;

import static com.supercilex.robotscouter.data.util.ScoutUtilsKt.getScoutKey;
import static com.supercilex.robotscouter.data.util.ScoutUtilsKt.getScoutKeyBundle;
import static com.supercilex.robotscouter.data.util.ScoutUtilsKt.getScoutMetricsRef;
import static com.supercilex.robotscouter.util.FirebaseAdapterUtilsKt.restoreRecyclerViewState;
import static com.supercilex.robotscouter.util.FirebaseAdapterUtilsKt.saveRecyclerViewState;

public class ScoutFragment extends Fragment {
    private ScoutAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private RecyclerView.LayoutManager mManager;
    private String mScoutKey;

    public static ScoutFragment newInstance(String scoutKey) {
        ScoutFragment fragment = new ScoutFragment();
        fragment.setArguments(getScoutKeyBundle(scoutKey));
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mScoutKey = getScoutKey(getArguments());
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        mRecyclerView = (RecyclerView) inflater.inflate(R.layout.fragment_scout, container, false);

        mManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mManager);
        mRecyclerView.setHasFixedSize(true);

        mAdapter = new ScoutAdapter(
                getScoutMetricsRef(mScoutKey),
                getChildFragmentManager(),
                mRecyclerView);
        mRecyclerView.setAdapter(mAdapter);
        restoreRecyclerViewState(savedInstanceState, mManager);

        return mRecyclerView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        saveRecyclerViewState(outState, mManager);
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
        RobotScouter.Companion.getRefWatcher(getActivity()).watch(this);
    }
}

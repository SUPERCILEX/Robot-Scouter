package com.supercilex.robotscouter.ui.scout;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.RobotScouter;
import com.supercilex.robotscouter.data.model.ScoutMetric;
import com.supercilex.robotscouter.ui.scout.viewholder.ScoutViewHolder;
import com.supercilex.robotscouter.util.BaseHelper;
import com.supercilex.robotscouter.util.Constants;

public class ScoutFragment extends Fragment { // NOPMD
    private FirebaseRecyclerAdapter<ScoutMetric, ScoutViewHolder> mAdapter;
    private LinearLayoutManager mManager;

    public static ScoutFragment newInstance(String key) {
        Bundle args = new Bundle();
        args.putString(Constants.SCOUT_KEY, key);
        ScoutFragment fragment = new ScoutFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.recycler_view, container, false);

        RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.list);
        recyclerView.setHasFixedSize(true);
        mManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(mManager);

        mAdapter = new ScoutAdapter(
                ScoutMetric.class,
                ScoutViewHolder.class,
                BaseHelper.getDatabase()
                        .child(Constants.FIREBASE_SCOUTS)
                        .child(getArguments().getString(Constants.SCOUT_KEY))
                        .child(Constants.FIREBASE_VIEWS),
                (SimpleItemAnimator) recyclerView.getItemAnimator());
        recyclerView.setAdapter(mAdapter);
        BaseHelper.restoreRecyclerViewState(savedInstanceState, mAdapter, mManager);
        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        BaseHelper.saveRecyclerViewState(outState, mAdapter, mManager);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mAdapter.cleanup();
        RobotScouter.getRefWatcher(getActivity()).watch(this);
    }
}

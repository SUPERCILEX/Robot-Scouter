package com.supercilex.robotscouter.ui.scout.template;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.Scout;
import com.supercilex.robotscouter.data.model.ScoutMetric;
import com.supercilex.robotscouter.ui.scout.ScoutFragment;
import com.supercilex.robotscouter.ui.scout.viewholder.ScoutViewHolderBase;
import com.supercilex.robotscouter.util.BaseHelper;
import com.supercilex.robotscouter.util.Constants;

public class ScoutTemplateFragment extends ScoutFragment {
    public static ScoutTemplateFragment newInstance(String key) {
        ScoutTemplateFragment fragment = new ScoutTemplateFragment();
        fragment.setArguments(Scout.getScoutKeyBundle(key));
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.recycler_view, container, false);

        RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.list);
        mManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(mManager);

        mAdapter = new ScoutTemplateAdapter(
                ScoutMetric.class,
                ScoutViewHolderBase.class,
                Constants.FIREBASE_SCOUT_TEMPLATES
                        .child(Scout.getScoutKey(getArguments()))
                        .child(Constants.FIREBASE_VIEWS),
                (SimpleItemAnimator) recyclerView.getItemAnimator());
        recyclerView.setAdapter(mAdapter);
        BaseHelper.restoreRecyclerViewState(savedInstanceState, mAdapter, mManager);

        return rootView;
    }
}

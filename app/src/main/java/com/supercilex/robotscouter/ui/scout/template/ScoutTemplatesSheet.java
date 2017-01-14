package com.supercilex.robotscouter.ui.scout.template;

import android.os.Bundle;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.RobotScouter;
import com.supercilex.robotscouter.data.model.ScoutMetric;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.data.util.FirebaseCopier;
import com.supercilex.robotscouter.ui.scout.viewholder.ScoutViewHolderBase;
import com.supercilex.robotscouter.util.BaseHelper;
import com.supercilex.robotscouter.util.Constants;

public class ScoutTemplatesSheet extends BottomSheetDialogFragment {
    private static final String TAG = "ScoutTemplatesSheet";

    private RecyclerView mRecyclerView;
    private ScoutTemplateAdapter mAdapter;
    private LinearLayoutManager mManager;

    public static void show(FragmentManager manager, Team team) {
        ScoutTemplatesSheet sheet = new ScoutTemplatesSheet();
        sheet.setArguments(team.getBundle());
        sheet.show(manager, TAG);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.recycler_view, container, false);

        final Team team = Team.getTeam(getArguments());
        String templateKey = team.getTemplateKey();
        if (TextUtils.isEmpty(templateKey)) {
            DatabaseReference newTemplateRef = Constants.FIREBASE_SCOUT_TEMPLATES.push();
            templateKey = newTemplateRef.getKey();
            final String finalTemplateKey = templateKey;
            new FirebaseCopier(Constants.FIREBASE_DEFAULT_TEMPLATE, newTemplateRef) {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    super.onDataChange(snapshot);
                    team.updateTemplateKey(finalTemplateKey);
                }
            }.performTransformation();
        }

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.list);
        mManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mManager);

        mAdapter = new ScoutTemplateAdapter(
                ScoutMetric.class,
                ScoutViewHolderBase.class,
                Constants.FIREBASE_SCOUT_TEMPLATES
                        .child(templateKey)
                        .child(Constants.FIREBASE_VIEWS),
                (SimpleItemAnimator) mRecyclerView.getItemAnimator());
        mRecyclerView.setAdapter(mAdapter);
        BaseHelper.restoreRecyclerViewState(savedInstanceState, mAdapter, mManager);

        new ItemTouchHelper(new ScoutTemplateItemTouchCallback(mAdapter))
                .attachToRecyclerView(mRecyclerView);

        return rootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mRecyclerView.clearFocus(); // Needed to ensure template is saved if user taps outside sheet
        mAdapter.cleanup();
        RobotScouter.getRefWatcher(getActivity()).watch(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        BaseHelper.saveRecyclerViewState(outState, mAdapter, mManager);
        super.onSaveInstanceState(outState);
    }
}

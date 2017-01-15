package com.supercilex.robotscouter.ui.scout.template;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.github.clans.fab.FloatingActionMenu;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.RobotScouter;
import com.supercilex.robotscouter.data.model.MetricType;
import com.supercilex.robotscouter.data.model.ScoutMetric;
import com.supercilex.robotscouter.data.model.ScoutSpinner;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.data.util.FirebaseCopier;
import com.supercilex.robotscouter.ui.scout.viewholder.ScoutViewHolderBase;
import com.supercilex.robotscouter.util.BaseHelper;
import com.supercilex.robotscouter.util.Constants;

import java.util.Collections;

public class ScoutTemplatesSheet extends BottomSheetDialogFragment
        implements View.OnClickListener, DialogInterface.OnShowListener, RecyclerView.OnItemTouchListener {
    private static final String TAG = "ScoutTemplatesSheet";

    private RecyclerView mRecyclerView;
    private ScoutTemplateAdapter mAdapter;
    private LinearLayoutManager mManager;
    private String mTemplateKey;
    private View mRootView;
    private FloatingActionMenu mFam;

    public static void show(FragmentManager manager, Team team) {
        ScoutTemplatesSheet sheet = new ScoutTemplatesSheet();
        sheet.setArguments(team.getBundle());
        sheet.show(manager, TAG);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(this);
        return dialog;
    }

    @Override
    public void onShow(DialogInterface dialog) {
        BottomSheetDialog d = (BottomSheetDialog) dialog;
        FrameLayout bottomSheet = (FrameLayout) d.findViewById(android.support.design.R.id.design_bottom_sheet);
        int parentHeight = getActivity().findViewById(android.R.id.content).getHeight();
        BottomSheetBehavior.from(bottomSheet).setPeekHeight(parentHeight);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_scout_template, container, false);

        getTemplateKey();
        setupRecyclerView(savedInstanceState);
        initFabMenu();

        return mRootView;
    }

    private void getTemplateKey() {
        final Team team = Team.getTeam(getArguments());
        mTemplateKey = team.getTemplateKey();
        if (TextUtils.isEmpty(mTemplateKey)) {
            DatabaseReference newTemplateRef = Constants.FIREBASE_SCOUT_TEMPLATES.push();
            mTemplateKey = newTemplateRef.getKey();
            new FirebaseCopier(Constants.FIREBASE_DEFAULT_TEMPLATE, newTemplateRef) {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    super.onDataChange(snapshot);
                    team.updateTemplateKey(mTemplateKey, getContext());
                }
            }.performTransformation();
        }
    }

    private void setupRecyclerView(Bundle savedInstanceState) {
        mRecyclerView = (RecyclerView) mRootView.findViewById(R.id.list);
        mManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mManager);

        mAdapter = new ScoutTemplateAdapter(
                ScoutMetric.class,
                ScoutViewHolderBase.class,
                Constants.FIREBASE_SCOUT_TEMPLATES
                        .child(mTemplateKey)
                        .child(Constants.FIREBASE_VIEWS),
                (SimpleItemAnimator) mRecyclerView.getItemAnimator(),
                mRootView);
        mRecyclerView.setAdapter(mAdapter);
        BaseHelper.restoreRecyclerViewState(savedInstanceState, mAdapter, mManager);

        new ItemTouchHelper(new ScoutTemplateItemTouchCallback(mAdapter))
                .attachToRecyclerView(mRecyclerView);
    }

    private void initFabMenu() {
        mFam = (FloatingActionMenu) mRootView.findViewById(R.id.fab_menu);

        mRootView.findViewById(R.id.add_checkbox).setOnClickListener(this);
        mRootView.findViewById(R.id.add_counter).setOnClickListener(this);
        mRootView.findViewById(R.id.add_spinner).setOnClickListener(this);
        mRootView.findViewById(R.id.add_note).setOnClickListener(this);

        // This lets us close the fam when the recyclerview it touched
        mRecyclerView.addOnItemTouchListener(this);
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        mFam.close(true);
        return false;
    }

    @Override
    public void onClick(View v) {
        DatabaseReference metricRef = Constants.FIREBASE_SCOUT_TEMPLATES
                .child(mTemplateKey)
                .child(Constants.FIREBASE_VIEWS)
                .push();
        int itemCount = mAdapter.getItemCount();
        switch (v.getId()) {
            case R.id.add_checkbox:
                metricRef.setValue(new ScoutMetric<>("", false, MetricType.CHECKBOX), itemCount);
                break;
            case R.id.add_counter:
                metricRef.setValue(new ScoutMetric<>("", 0, MetricType.COUNTER), itemCount);
                break;
            case R.id.add_spinner:
                metricRef.setValue(
                        new ScoutSpinner("", Collections.singletonList("item 1"), 0),
                        itemCount);
                break;
            case R.id.add_note:
                metricRef.setValue(new ScoutMetric<>("", "", MetricType.NOTE), itemCount);
                break;
        }
        mAdapter.addItemToScrollQueue(itemCount);
        mFam.close(true);
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

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {
        // We only care about onInterceptTouchEvent
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        // We only care about onInterceptTouchEvent
    }
}

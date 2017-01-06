package com.supercilex.robotscouter.ui.scout;

import android.support.v7.widget.SimpleItemAnimator;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.Query;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.MetricType;
import com.supercilex.robotscouter.data.model.ScoutMetric;
import com.supercilex.robotscouter.data.model.ScoutSpinner;
import com.supercilex.robotscouter.ui.scout.viewholder.CheckboxViewHolder;
import com.supercilex.robotscouter.ui.scout.viewholder.CounterViewHolder;
import com.supercilex.robotscouter.ui.scout.viewholder.EditTextViewHolder;
import com.supercilex.robotscouter.ui.scout.viewholder.ScoutViewHolderBase;
import com.supercilex.robotscouter.ui.scout.viewholder.SpinnerViewHolder;
import com.supercilex.robotscouter.util.Constants;

import java.util.ArrayList;

public class ScoutAdapter extends FirebaseRecyclerAdapter<ScoutMetric, ScoutViewHolderBase> {
    private SimpleItemAnimator mAnimator;

    public ScoutAdapter(Class<ScoutMetric> modelClass,
                        Class<ScoutViewHolderBase> viewHolderClass,
                        Query query,
                        SimpleItemAnimator animator) {
        super(modelClass, 0, viewHolderClass, query);
        mAnimator = animator;
    }

    @Override
    public void populateViewHolder(ScoutViewHolderBase viewHolder,
                                   ScoutMetric metric,
                                   int position) {
        //noinspection unchecked
        viewHolder.bind(metric, getRef(position), mAnimator);
        mAnimator.setSupportsChangeAnimations(true);
    }

    @Override
    public ScoutViewHolderBase onCreateViewHolder(ViewGroup parent, @MetricType int viewType) {
        switch (viewType) {
            case MetricType.CHECKBOX:
                return new CheckboxViewHolder(
                        LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.scout_checkbox,
                                         parent,
                                         false));
            case MetricType.COUNTER:
                return new CounterViewHolder(
                        LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.scout_counter,
                                         parent,
                                         false));
            case MetricType.EDIT_TEXT:
                return new EditTextViewHolder(
                        LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.scout_notes,
                                         parent,
                                         false));
            case MetricType.SPINNER:
                return new SpinnerViewHolder(
                        LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.scout_spinner,
                                         parent,
                                         false));
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    protected ScoutMetric parseSnapshot(DataSnapshot snapshot) {
        switch (snapshot.child(Constants.FIREBASE_TYPE).getValue(Integer.class)) {
            case MetricType.CHECKBOX:
                return snapshot.getValue(new GenericTypeIndicator<ScoutMetric<Boolean>>() {
                });
            case MetricType.COUNTER:
                return snapshot.getValue(new GenericTypeIndicator<ScoutMetric<Integer>>() {
                });
            case MetricType.EDIT_TEXT:
                return snapshot.getValue(new GenericTypeIndicator<ScoutMetric<String>>() {
                });
            case MetricType.SPINNER:
                return new ScoutSpinner(
                        snapshot.child(Constants.FIREBASE_NAME).getValue(String.class),
                        snapshot.child(Constants.FIREBASE_VALUE)
                                .getValue(new GenericTypeIndicator<ArrayList<String>>() {
                                }),
                        snapshot.child(Constants.FIREBASE_SELECTED_VALUE).getValue(Integer.class));
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    @MetricType
    public int getItemViewType(int position) {
        return getItem(position).getType();
    }

    @Override
    public void onCancelled(DatabaseError error) {
        FirebaseCrash.report(error.toException());
    }
}

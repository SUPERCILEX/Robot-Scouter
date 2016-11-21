package com.supercilex.robotscouter.ui.scout;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.Query;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.ScoutMetric;
import com.supercilex.robotscouter.data.model.ScoutSpinner;
import com.supercilex.robotscouter.ui.scout.viewholder.CheckboxViewHolder;
import com.supercilex.robotscouter.ui.scout.viewholder.CounterViewHolder;
import com.supercilex.robotscouter.ui.scout.viewholder.EditTextViewHolder;
import com.supercilex.robotscouter.ui.scout.viewholder.ScoutViewHolder;
import com.supercilex.robotscouter.ui.scout.viewholder.SpinnerViewHolder;
import com.supercilex.robotscouter.util.Constants;

import java.util.ArrayList;

public class ScoutAdapter extends FirebaseRecyclerAdapter<ScoutMetric, ScoutViewHolder> {
    public ScoutAdapter(Class<ScoutMetric> modelClass,
                        int modelLayout,
                        Class<ScoutViewHolder> viewHolderClass,
                        Query ref) {
        super(modelClass, modelLayout, viewHolderClass, ref);
    }

    @Override
    public void populateViewHolder(ScoutViewHolder viewHolder, ScoutMetric view, int position) {
        viewHolder.bind(view, getRef(position));
    }

    @Override
    public ScoutViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case Constants.CHECKBOX:
                return new CheckboxViewHolder(LayoutInflater.from(parent.getContext())
                                                      .inflate(R.layout.scout_checkbox,
                                                               parent,
                                                               false));
            case Constants.COUNTER:
                return new CounterViewHolder(LayoutInflater.from(parent.getContext())
                                                     .inflate(R.layout.scout_counter,
                                                              parent,
                                                              false));
            case Constants.EDIT_TEXT:
                return new EditTextViewHolder(LayoutInflater.from(parent.getContext())
                                                      .inflate(R.layout.scout_notes,
                                                               parent,
                                                               false));
            case Constants.SPINNER:
                return new SpinnerViewHolder(LayoutInflater.from(parent.getContext())
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
            case Constants.CHECKBOX:
                return snapshot.getValue(new GenericTypeIndicator<ScoutMetric<Boolean>>() {
                });
            case Constants.COUNTER:
                return snapshot.getValue(new GenericTypeIndicator<ScoutMetric<Integer>>() {
                });
            case Constants.EDIT_TEXT:
                return snapshot.getValue(new GenericTypeIndicator<ScoutMetric<String>>() {
                });
            case Constants.SPINNER:
                // return snapshot.getValue(new GenericTypeIndicator<ScoutSpinner>() {}); NOPMD
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
    public int getItemViewType(int position) {
        return getItem(position).getType();
    }

    @Override
    protected void onCancelled(DatabaseError databaseError) {
        FirebaseCrash.report(databaseError.toException());
    }
}

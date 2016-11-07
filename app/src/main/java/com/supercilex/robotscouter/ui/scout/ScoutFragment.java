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
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.GenericTypeIndicator;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.RobotScouter;
import com.supercilex.robotscouter.data.model.ScoutMetric;
import com.supercilex.robotscouter.data.model.ScoutSpinner;
import com.supercilex.robotscouter.ui.scout.viewholder.CheckboxViewHolder;
import com.supercilex.robotscouter.ui.scout.viewholder.CounterViewHolder;
import com.supercilex.robotscouter.ui.scout.viewholder.EditTextViewHolder;
import com.supercilex.robotscouter.ui.scout.viewholder.ScoutViewHolder;
import com.supercilex.robotscouter.ui.scout.viewholder.SpinnerViewHolder;
import com.supercilex.robotscouter.util.BaseHelper;
import com.supercilex.robotscouter.util.Constants;

import java.util.ArrayList;

public class ScoutFragment extends Fragment {
    private static final String ARG_SCOUT_KEY = "scout_key";

    // TODO: 11/06/2016 copy commit 02cc02ed49ae89be6d820ce5405aca5f579e1476
    private FirebaseRecyclerAdapter<ScoutMetric, ScoutViewHolder> mAdapter;

    public static ScoutFragment newInstance(String key) {
        ScoutFragment fragment = new ScoutFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SCOUT_KEY, key);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mAdapter.cleanup();
        RobotScouter.getRefWatcher(getActivity()).watch(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.current_scout_fragment, container, false);

        final RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.scout_data);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // TODO: 09/22/2016 fix this
        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);

        mAdapter = new FirebaseRecyclerAdapter<ScoutMetric, ScoutViewHolder>(
                ScoutMetric.class,
                R.layout.activity_team_list_row_layout,
                ScoutViewHolder.class,
                BaseHelper.getDatabase()
                        .child(Constants.FIREBASE_SCOUTS)
                        .child(getArguments().getString(ARG_SCOUT_KEY))
                        .child(Constants.FIREBASE_VIEWS)) {
            @Override
            public void populateViewHolder(ScoutViewHolder viewHolder,
                                           ScoutMetric view,
                                           int position) {
                viewHolder.initialize(view, getRef(position));
            }

            @Override
            protected void onCancelled(DatabaseError databaseError) {
                FirebaseCrash.report(databaseError.toException());
            }

            @Override
            protected ScoutMetric parseSnapshot(DataSnapshot snapshot) {
                int viewType = snapshot.child(Constants.FIREBASE_TYPE).getValue(Integer.class);

                if (viewType == Constants.CHECKBOX) {
                    return snapshot.getValue(new GenericTypeIndicator<ScoutMetric<Boolean>>() {
                    });
                } else if (viewType == Constants.COUNTER) {
                    return snapshot.getValue(new GenericTypeIndicator<ScoutMetric<Integer>>() {
                    });
                } else if (viewType == Constants.SPINNER) {
//                    return snapshot.getValue(new GenericTypeIndicator<ScoutSpinner>() {
//                    });
                    return new ScoutSpinner(
                            snapshot.child(Constants.FIREBASE_NAME).getValue(String.class),
                            snapshot.child(Constants.FIREBASE_VALUE)
                                    .getValue(new GenericTypeIndicator<ArrayList<String>>() {
                                    }),
                            snapshot.child(Constants.FIREBASE_SELECTED_VALUE)
                                    .getValue(Integer.class)
                    ).setType(Constants.SPINNER);
                } else if (viewType == Constants.EDIT_TEXT) {
                    return snapshot.getValue(new GenericTypeIndicator<ScoutMetric<String>>() {
                    });
                }

                throw new IllegalArgumentException("Scout class not found at parseSnapshot");
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
                    case Constants.SPINNER:
                        return new SpinnerViewHolder(LayoutInflater.from(parent.getContext())
                                                             .inflate(R.layout.scout_spinner,
                                                                      parent,
                                                                      false));
                    case Constants.EDIT_TEXT:
                        return new EditTextViewHolder(LayoutInflater.from(parent.getContext())
                                                              .inflate(R.layout.scout_notes,
                                                                       parent,
                                                                       false));
                    default:
                        throw new IllegalStateException();
                }
            }

            @Override
            public int getItemViewType(int position) {
                return getItem(position).getType();
            }
        };

        recyclerView.setAdapter(mAdapter);
        return rootView;
    }
}

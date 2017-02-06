package com.supercilex.robotscouter.ui.scout.template;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;

import com.firebase.ui.database.adapter.FirebaseRecyclerAdapter;
import com.google.firebase.database.DatabaseReference;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.RobotScouter;
import com.supercilex.robotscouter.ui.scout.viewholder.template.SpinnerItemViewHolder;
import com.supercilex.robotscouter.util.DatabaseHelper;
import com.supercilex.robotscouter.util.FirebaseAdapterHelper;

public class SpinnerTemplateDialog extends DialogFragment implements View.OnClickListener {
    private static final String TAG = "SpinnerTemplateDialog";
    private static final String QUERY_KEY = "SpinnerTemplateDialog";

    private View mRootView;
    private RecyclerView.LayoutManager mManager;
    private FirebaseRecyclerAdapter<String, SpinnerItemViewHolder> mAdapter;
    private ScoutTemplateItemTouchCallback<String, SpinnerItemViewHolder> mItemTouchCallback;
    private DatabaseReference mRef;

    public static void show(FragmentManager manager, DatabaseReference ref) {
        SpinnerTemplateDialog dialog = new SpinnerTemplateDialog();

        Bundle bundle = new Bundle();
        bundle.putString(QUERY_KEY, ref.toString().split("firebaseio.com/")[1]);
        dialog.setArguments(bundle);

        dialog.show(manager, TAG);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRootView = View.inflate(getContext(), R.layout.scout_template_edit_spinner_items, null);
        mRootView.findViewById(R.id.fab).setOnClickListener(this);
        setupRecyclerView(savedInstanceState);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getContext())
                .setTitle(R.string.edit_spinner_items)
                .setView(mRootView)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        FirebaseAdapterHelper.saveRecyclerViewState(outState, mAdapter, mManager);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mAdapter.cleanup();
        RobotScouter.getRefWatcher(getActivity()).watch(this);
    }

    @Override
    public void onClick(View v) {
        mRef.child(String.valueOf(mAdapter.getItemCount()))
                .setValue("item " + (mAdapter.getItemCount() + 1), mAdapter.getItemCount());
        mItemTouchCallback.addItemToScrollQueue(mAdapter.getItemCount());
    }

    private void setupRecyclerView(@Nullable Bundle savedInstanceState) {
        RecyclerView recyclerView = (RecyclerView) mRootView.findViewById(R.id.list);
        mManager = new LinearLayoutManager(getContext());
        mItemTouchCallback = new ScoutTemplateItemTouchCallback<>(recyclerView);
        ItemTouchHelper touchHelper = new ItemTouchHelper(mItemTouchCallback);

        recyclerView.setLayoutManager(mManager);
        mItemTouchCallback.setItemTouchHelper(touchHelper);
        touchHelper.attachToRecyclerView(recyclerView);

        mRef = DatabaseHelper.getDatabase().getReference(getArguments().getString(QUERY_KEY));
        mAdapter = new FirebaseRecyclerAdapter<String, SpinnerItemViewHolder>(
                String.class,
                R.layout.scout_template_spinner_item,
                SpinnerItemViewHolder.class,
                mRef) {
            @Override
            protected void populateViewHolder(SpinnerItemViewHolder viewHolder,
                                              String itemText,
                                              int position) {
                viewHolder.bind(itemText, getRef(position));
                mItemTouchCallback.updateDragStatus(viewHolder, position);
            }

            @Override
            public void onChildChanged(EventType type,
                                       int index,
                                       int oldIndex) {
                if (mItemTouchCallback.onChildChanged(type, index)) {
                    super.onChildChanged(type, index, oldIndex);
                }
            }
        };
        recyclerView.setAdapter(mAdapter);
        mItemTouchCallback.setAdapter(mAdapter);
        FirebaseAdapterHelper.restoreRecyclerViewState(savedInstanceState, mAdapter, mManager);
    }
}

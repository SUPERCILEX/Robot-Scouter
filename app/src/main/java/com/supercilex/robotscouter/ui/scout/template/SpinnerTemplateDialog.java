package com.supercilex.robotscouter.ui.scout.template;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.RobotScouter;
import com.supercilex.robotscouter.ui.scout.viewholder.template.SpinnerItemViewHolder;
import com.supercilex.robotscouter.util.Constants;
import com.supercilex.robotscouter.util.DatabaseHelper;
import com.supercilex.robotscouter.util.FirebaseAdapterHelper;

public class SpinnerTemplateDialog extends DialogFragment implements View.OnClickListener, DialogInterface.OnShowListener {
    private static final String TAG = "SpinnerTemplateDialog";

    private View mRootView;
    private String mSelectedValueKey;

    private RecyclerView.LayoutManager mManager;
    private FirebaseRecyclerAdapter<String, SpinnerItemViewHolder> mAdapter;
    private ScoutTemplateItemTouchCallback<String, SpinnerItemViewHolder> mItemTouchCallback;
    private DatabaseReference mRef;

    public static void show(FragmentManager manager,
                            DatabaseReference ref,
                            String selectedValueIndex) {
        SpinnerTemplateDialog dialog = new SpinnerTemplateDialog();

        Bundle args = DatabaseHelper.getRefBundle(ref);
        args.putString(Constants.FIREBASE_SELECTED_VALUE_KEY, selectedValueIndex);
        dialog.setArguments(args);

        dialog.show(manager, TAG);

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRootView = View.inflate(getContext(), R.layout.scout_template_edit_spinner_items, null);
        mRootView.findViewById(R.id.fab).setOnClickListener(this);
        setupRecyclerView(savedInstanceState);

        mSelectedValueKey = getArguments().getString(Constants.FIREBASE_SELECTED_VALUE_KEY);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(R.string.edit_spinner_items)
                .setView(mRootView)
                .setPositiveButton(android.R.string.ok, null)
                .create();
        dialog.setOnShowListener(this);
        return dialog;
    }

    @Override
    public void onShow(DialogInterface dialog) {
        ((Dialog) dialog).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
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
        mRootView.findViewById(R.id.list).clearFocus();
        RobotScouter.getRefWatcher(getActivity()).watch(this);
    }

    @Override
    public void onClick(View v) {
        int itemCount = mAdapter.getItemCount();
        mRef.push().setValue("item " + (itemCount + 1),
                             FirebaseAdapterHelper.getHighestIntPriority(mAdapter.getSnapshots()));
        mItemTouchCallback.addItemToScrollQueue(itemCount);
    }

    private void setupRecyclerView(@Nullable Bundle savedInstanceState) {
        RecyclerView recyclerView = (RecyclerView) mRootView.findViewById(R.id.list);
        mManager = new LinearLayoutManager(getContext());
        mItemTouchCallback = new ScoutTemplateItemTouchCallback<>(recyclerView);
        ItemTouchHelper touchHelper = new ItemTouchHelper(mItemTouchCallback);

        recyclerView.setLayoutManager(mManager);
        mItemTouchCallback.setItemTouchHelper(touchHelper);
        touchHelper.attachToRecyclerView(recyclerView);

        mRef = DatabaseHelper.getRef(getArguments());
        mAdapter = new FirebaseRecyclerAdapter<String, SpinnerItemViewHolder>(
                String.class,
                R.layout.scout_template_spinner_item,
                SpinnerItemViewHolder.class,
                mRef) {
            @Override
            protected void populateViewHolder(SpinnerItemViewHolder viewHolder,
                                              String itemText,
                                              int position) {
                viewHolder.bind(itemText, mSnapshots.get(position));
                mItemTouchCallback.onBind(viewHolder, position);
            }

            @Override
            public void onChildChanged(EventType type,
                                       DataSnapshot snapshot,
                                       int index,
                                       int oldIndex) {
                if (type == EventType.REMOVED) {
                    if (getItemCount() == 0) {
                        dismiss();
                        mRef.getParent().removeValue();
                        return;
                    }

                    if (TextUtils.equals(mSelectedValueKey, snapshot.getKey())) {
                        mRef.getParent().child(Constants.FIREBASE_SELECTED_VALUE_KEY).removeValue();
                    }
                }

                if (type == EventType.ADDED && snapshot.getPriority() == null) {
                    snapshot.getRef().setPriority(index);
                }

                if (mItemTouchCallback.onChildChanged(type, index)) {
                    super.onChildChanged(type, snapshot, index, oldIndex);
                }
            }
        };
        recyclerView.setAdapter(mAdapter);
        mItemTouchCallback.setAdapter(mAdapter);
        FirebaseAdapterHelper.restoreRecyclerViewState(savedInstanceState, mAdapter, mManager);
    }
}

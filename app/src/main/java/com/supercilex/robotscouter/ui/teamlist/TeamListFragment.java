package com.supercilex.robotscouter.ui.teamlist;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.FirebaseAuth;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.RobotScouter;
import com.supercilex.robotscouter.data.model.Team;

import static com.supercilex.robotscouter.util.FirebaseAdapterUtilsKt.restoreRecyclerViewState;
import static com.supercilex.robotscouter.util.FirebaseAdapterUtilsKt.saveRecyclerViewState;

/**
 * Created in {@link R.layout#activity_team_list}
 */
public class TeamListFragment extends Fragment implements FirebaseAuth.AuthStateListener, OnBackPressedListener {
    public static final String TAG = "TeamListFragment";

    private Bundle mSavedInstanceState;

    private RecyclerView mRecyclerView;
    private TeamMenuHelper mMenuHelper;

    private TeamListAdapter mAdapter;
    private TaskCompletionSource<TeamListAdapter> mOnAdapterReadyTask = new TaskCompletionSource<>();
    private RecyclerView.LayoutManager mManager;
    private FloatingActionButton mFab;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mMenuHelper = new TeamMenuHelper(this);
    }

    @Override
    public void onAuthStateChanged(@NonNull FirebaseAuth auth) {
        cleanup();
        if (mFab != null) mFab.show();
        if (auth.getCurrentUser() == null) {
            View view = getView();
            if (view != null) view.findViewById(R.id.no_content_hint).setVisibility(View.VISIBLE);
        } else {
            mAdapter = new TeamListAdapter(this, mMenuHelper, mSavedInstanceState);
            mOnAdapterReadyTask.setResult(mAdapter);
            mOnAdapterReadyTask = new TaskCompletionSource<>();

            mMenuHelper.setAdapter(mAdapter);
            mRecyclerView.setAdapter(mAdapter);
            mMenuHelper.restoreState(mSavedInstanceState);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mSavedInstanceState = savedInstanceState;
        View rootView = inflater.inflate(R.layout.fragment_team_list, container, false);
        mRecyclerView = rootView.findViewById(R.id.list);
        mManager = new LinearLayoutManager(getContext());

        mMenuHelper.setRecyclerView(mRecyclerView);

        mRecyclerView.setLayoutManager(mManager);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    // User scrolled down -> hide the FAB
                    mFab.hide();
                } else if (dy < 0 && mMenuHelper.noItemsSelected()) {
                    mFab.show();
                }
            }
        });
        restoreRecyclerViewState(mSavedInstanceState, mManager);

        FirebaseAuth.getInstance().addAuthStateListener(this);

        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mFab = getActivity().findViewById(R.id.fab);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        saveRecyclerViewState(outState, mManager);
        if (mAdapter != null) mAdapter.onSaveInstanceState(outState);
        mMenuHelper.saveState(outState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        FirebaseAuth.getInstance().removeAuthStateListener(this);
        cleanup();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        RobotScouter.Companion.getRefWatcher(getActivity()).watch(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        mMenuHelper.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mMenuHelper.onOptionsItemSelected(item);
    }

    public void selectTeam(@Nullable Team team) {
        String teamKey = team == null ? null : team.getKey();
        if (mAdapter == null) {
            mOnAdapterReadyTask.getTask()
                    .addOnSuccessListener(adapter -> adapter.updateSelection(teamKey));
        } else {
            mAdapter.updateSelection(teamKey);
        }
    }

    private void cleanup() {
        if (mAdapter != null) {
            mAdapter.cleanup();
            mAdapter.notifyDataSetChanged();
        }
        mRecyclerView.setAdapter(null);
    }

    /**
     * @see MenuManager#onBackPressed()
     */
    @Override
    public boolean onBackPressed() {
        return mMenuHelper.onBackPressed();
    }

    /**
     * @see MenuManager#resetMenu()
     */
    public void resetMenu() {
        mMenuHelper.resetMenu();
    }

    /**
     * Used in {@link com.supercilex.robotscouter.data.client.spreadsheet.ExportService#exportAndShareSpreadSheet(Fragment,
     * com.supercilex.robotscouter.ui.PermissionRequestHandler, java.util.List)}
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        mMenuHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mMenuHelper.onActivityResult(requestCode);
    }
}

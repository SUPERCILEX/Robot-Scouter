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

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.crash.FirebaseCrash;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.RobotScouter;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.ui.common.OnBackPressedListener;
import com.supercilex.robotscouter.util.FirebaseAdapterHelper;

import java.util.List;

/**
 * Created in {@link R.layout#activity_team_list}
 */
public class TeamListFragment extends Fragment implements FirebaseAuth.AuthStateListener, OnBackPressedListener {
    private Bundle mSavedInstanceState;

    private RecyclerView mRecyclerView;
    private TeamMenuHelper mMenuHelper;

    private FirebaseRecyclerAdapter<Team, TeamViewHolder> mAdapter;
    private RecyclerView.LayoutManager mManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mMenuHelper = new TeamMenuHelper(this);
    }

    @Override
    public void onAuthStateChanged(@NonNull FirebaseAuth auth) {
        cleanup();
        if (auth.getCurrentUser() != null) {
            // Log uid to help debug db crashes
            FirebaseCrash.log(auth.getCurrentUser().getUid());


            mManager = new LinearLayoutManager(getContext());
            mAdapter = new TeamListAdapter(this, mMenuHelper);

            mMenuHelper.setAdapter(mAdapter);
            mRecyclerView.setLayoutManager(mManager);
            mRecyclerView.setAdapter(mAdapter);

            FirebaseAdapterHelper.restoreRecyclerViewState(mSavedInstanceState, mAdapter, mManager);
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
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.list);

        mMenuHelper.setRecyclerView(mRecyclerView);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                FloatingActionButton fab = (FloatingActionButton) getActivity().findViewById(R.id.fab);
                if (dy > 0 && fab.getVisibility() == View.VISIBLE) {
                    // User scrolled down and the FAB is currently visible -> hide the FAB
                    fab.hide();
                } else if (dy < 0 && fab.getVisibility() != View.VISIBLE && mMenuHelper.noItemsSelected()) {
                    fab.show();
                }
            }
        });

        FirebaseAuth.getInstance().addAuthStateListener(this);

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        FirebaseAdapterHelper.saveRecyclerViewState(outState, mAdapter, mManager);
        mMenuHelper.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        FirebaseAuth.getInstance().removeAuthStateListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cleanup();
        RobotScouter.getRefWatcher(getActivity()).watch(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        mMenuHelper.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mMenuHelper.onOptionsItemSelected(item);
    }

    private void cleanup() {
        mRecyclerView.setAdapter(null);
        if (mAdapter != null) mAdapter.cleanup();
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
     * Used in {@link SpreadsheetWriter#writeAndShareTeams(Fragment, List)}
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mMenuHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mMenuHelper.onActivityResult(requestCode, resultCode, data);
    }
}

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
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.RobotScouter;
import com.supercilex.robotscouter.ui.MenuManager;
import com.supercilex.robotscouter.util.BaseHelper;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Created in {@link R.layout#activity_team_list}
 */
public class TeamListFragment extends Fragment implements FirebaseAuth.AuthStateListener {
    private Bundle mSavedInstanceState;

    private TeamMenuHelper mMenuHelper;
    private RecyclerView mRecyclerView;
    private FirebaseRecyclerAdapter mAdapter;
    private WeakReference<LinearLayoutManager> mManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        mMenuHelper = new TeamMenuHelper(this);
        FirebaseAuth.getInstance().addAuthStateListener(this);
    }

    @Override
    public void onAuthStateChanged(@NonNull FirebaseAuth auth) {
        cleanup();
        if (auth.getCurrentUser() != null) {
            initAdapter();
            if (mRecyclerView != null) mRecyclerView.setAdapter(mAdapter);
            BaseHelper.restoreRecyclerViewState(mSavedInstanceState, mAdapter, mManager.get());
            mMenuHelper.restoreState(mSavedInstanceState);
            mSavedInstanceState = null; // NOPMD
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mSavedInstanceState = savedInstanceState;
        mRecyclerView = (RecyclerView) inflater.inflate(R.layout.recycler_view, container, false);
        mMenuHelper.setRecyclerView(mRecyclerView);
        mRecyclerView.setHasFixedSize(true);
        mManager = new WeakReference<>(new LinearLayoutManager(getContext()));
        mRecyclerView.setLayoutManager(mManager.get());
        mRecyclerView.setAdapter(mAdapter);

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

        return mRecyclerView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        BaseHelper.saveRecyclerViewState(outState, mAdapter, mManager.get());
        mMenuHelper.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mRecyclerView.setAdapter(null);
        mRecyclerView.setLayoutManager(null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cleanup();
        FirebaseAuth.getInstance().removeAuthStateListener(this);
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

    private void initAdapter() {
        mAdapter = new TeamListAdapter(this, mMenuHelper);
        mMenuHelper.setAdapter(mAdapter);
    }

    private void cleanup() {
        if (mAdapter != null) {
            mAdapter.cleanup();
        }
        mRecyclerView.setAdapter(null);
    }

    /**
     * @see MenuManager#onBackPressed()
     */
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

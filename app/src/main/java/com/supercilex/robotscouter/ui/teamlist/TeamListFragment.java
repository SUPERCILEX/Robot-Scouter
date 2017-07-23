package com.supercilex.robotscouter.ui.teamlist;

import android.arch.lifecycle.LifecycleFragment;
import android.arch.lifecycle.ViewModelProviders;
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

import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.RobotScouter;
import com.supercilex.robotscouter.data.model.Team;

import static com.supercilex.robotscouter.util.ConstantsKt.getTeamsListener;

/**
 * Created in {@link R.layout#activity_team_list}
 */
public class TeamListFragment extends LifecycleFragment implements OnBackPressedListener {
    public static final String TAG = "TeamListFragment";

    private TeamListHolder mHolder;

    private RecyclerView mRecyclerView;
    private TeamMenuHelper mMenuHelper;

    private TeamListAdapter mAdapter;
    private FloatingActionButton mFab;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mMenuHelper = new TeamMenuHelper(this);

        mHolder = ViewModelProviders.of(this).get(TeamListHolder.class);
        mHolder.init(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_team_list, container, false);
        mRecyclerView = rootView.findViewById(R.id.list);

        mMenuHelper.setRecyclerView(mRecyclerView);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
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

        getTeamsListener().observe(this, snapshots -> {
            cleanup();
            if (snapshots != null) {
                mAdapter = new TeamListAdapter(
                        snapshots, this, mMenuHelper, mHolder.getSelectedTeamKeyListener());
                mMenuHelper.setAdapter(mAdapter);
                mRecyclerView.setAdapter(mAdapter);
                mMenuHelper.restoreState(savedInstanceState);
            }
        });

        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mFab = getActivity().findViewById(R.id.fab);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        mHolder.onSaveInstanceState(outState);
        mMenuHelper.saveState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cleanup();
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
        mHolder.selectTeam(team);
    }

    private void cleanup() {
        if (mAdapter != null) {
            mAdapter.cleanup();
            mAdapter.notifyDataSetChanged();
        }
        mRecyclerView.setAdapter(null);
    }

    /**
     * @see TeamMenuHelper#onBackPressed()
     */
    @Override
    public boolean onBackPressed() {
        return mMenuHelper.onBackPressed();
    }

    /**
     * @see TeamMenuHelper#resetMenu()
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

package com.supercilex.robotscouter.ui.scout;

import android.app.ActivityManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.gms.tasks.Task;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.util.TeamHelper;
import com.supercilex.robotscouter.ui.teamlist.TeamListActivity;

public class ActivityScoutListFragment extends ScoutListFragmentBase {
    public static ScoutListFragmentBase newInstance(TeamHelper teamHelper, boolean addScout) {
        return setArgs(teamHelper, addScout, new ActivityScoutListFragment());
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setSupportActionBar((Toolbar) getView().findViewById(R.id.toolbar));
        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    protected AppBarViewHolderBase newAppBarViewHolder(TeamHelper teamHelper,
                                                       Task<Void> onScoutingReadyTask) {
        return new ActivityAppBarViewHolder(teamHelper, onScoutingReadyTask);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.scout, menu);
        mHolder.initMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (NavUtils.shouldUpRecreateTask(
                    getActivity(), new Intent(getContext(), TeamListActivity.class))) {
                TaskStackBuilder.create(getContext())
                        .addParentStack(getActivity())
                        .startActivities();
                getActivity().finish();
            } else {
                NavUtils.navigateUpFromSameTask(getActivity());
            }
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onTeamDeleted() {
        getActivity().finish();
    }

    private class ActivityAppBarViewHolder extends AppBarViewHolderBase {
        public ActivityAppBarViewHolder(TeamHelper teamHelper, Task<Void> onScoutingReadyTask) {
            super(teamHelper, ActivityScoutListFragment.this, onScoutingReadyTask);
        }

        @Override
        protected void bind() {
            super.bind();
            ((AppCompatActivity) getActivity()).getSupportActionBar()
                    .setTitle(mTeamHelper.toString());
            setTaskDescription(null, ContextCompat.getColor(getContext(), R.color.colorPrimary));
        }

        @Override
        protected void updateScrim(int color, Bitmap bitmap) {
            super.updateScrim(color, bitmap);
            mHeader.setStatusBarScrimColor(color);
            setTaskDescription(bitmap, color);
        }

        private void setTaskDescription(@Nullable Bitmap icon, @ColorInt int colorPrimary) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                    && (icon == null || !icon.isRecycled())
                    && getActivity() != null) {
                getActivity().setTaskDescription(
                        new ActivityManager.TaskDescription(mTeamHelper.toString(),
                                                            icon,
                                                            colorPrimary));
            }
        }
    }
}

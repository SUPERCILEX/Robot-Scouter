package com.supercilex.robotscouter.ui.scout;

import android.app.Activity;
import android.app.ActivityManager;
import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.gms.tasks.Task;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.util.TeamHelper;
import com.supercilex.robotscouter.ui.teamlist.TeamListActivity;

import static com.supercilex.robotscouter.util.ViewUtilsKt.isInTabletMode;

public class ActivityScoutListFragment extends ScoutListFragmentBase {
    public static ScoutListFragmentBase newInstance(Bundle args) {
        return setArgs(new ActivityScoutListFragment(), args);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (getActivity().getCallingActivity() != null && isInTabletMode(getContext())) {
            FragmentActivity activity = getActivity();
            activity.setResult(
                    Activity.RESULT_OK, new Intent().putExtra(KEY_SCOUT_ARGS, getBundle()));
            activity.finish();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setSupportActionBar(mRootView.findViewById(R.id.toolbar));
        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    protected AppBarViewHolderBase newAppBarViewHolder(LiveData<TeamHelper> listener,
                                                       Task<Void> onScoutingReadyTask) {
        return new ActivityAppBarViewHolder(listener, onScoutingReadyTask);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.scout, menu);
        mViewHolder.initMenu(menu);
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
        public ActivityAppBarViewHolder(LiveData<TeamHelper> listener,
                                        Task<Void> onScoutingReadyTask) {
            super(listener, ActivityScoutListFragment.this, mRootView, onScoutingReadyTask);
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

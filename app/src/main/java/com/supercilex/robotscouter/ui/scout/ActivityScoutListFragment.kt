package com.supercilex.robotscouter.ui.scout

import android.app.Activity
import android.app.ActivityManager
import android.arch.lifecycle.LiveData
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.support.annotation.ColorInt
import android.support.v4.app.NavUtils
import android.support.v4.app.TaskStackBuilder
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import com.google.android.gms.tasks.Task
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.util.TeamHelper
import com.supercilex.robotscouter.ui.teamlist.TeamListActivity
import com.supercilex.robotscouter.util.isInTabletMode

class ActivityScoutListFragment : ScoutListFragmentBase() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        if (activity.callingActivity != null && isInTabletMode(context)) {
            activity.setResult(
                    Activity.RESULT_OK, Intent().putExtra(ScoutListFragmentBase.KEY_SCOUT_ARGS, bundle))
            activity.finish()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        val activity = activity as AppCompatActivity
        activity.setSupportActionBar(mRootView.findViewById(R.id.toolbar))
        activity.supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        super.onActivityCreated(savedInstanceState)
    }

    override fun newAppBarViewHolder(listener: LiveData<TeamHelper>,
                                     onScoutingReadyTask: Task<Void>): AppBarViewHolderBase =
            ActivityAppBarViewHolder(listener, onScoutingReadyTask)

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.scout, menu)
        mViewHolder.initMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = if (item.itemId == android.R.id.home) {
        if (NavUtils.shouldUpRecreateTask(
                activity, Intent(context, TeamListActivity::class.java))) {
            TaskStackBuilder.create(context).addParentStack(activity).startActivities()
            activity.finish()
        } else {
            NavUtils.navigateUpFromSameTask(activity)
        }
        true
    } else {
        super.onOptionsItemSelected(item)
    }

    override fun onTeamDeleted() = activity.finish()

    private inner class ActivityAppBarViewHolder(listener: LiveData<TeamHelper>,
                                                 onScoutingReadyTask: Task<Void>) :
            AppBarViewHolderBase(listener, this@ActivityScoutListFragment, mRootView, onScoutingReadyTask) {
        override fun bind() {
            super.bind()
            (activity as AppCompatActivity).supportActionBar!!.title = mTeamHelper.toString()
            setTaskDescription(null, ContextCompat.getColor(context, R.color.colorPrimary))
        }

        override fun updateScrim(color: Int, bitmap: Bitmap) {
            super.updateScrim(color, bitmap)
            mHeader.setStatusBarScrimColor(color)
            setTaskDescription(bitmap, color)
        }

        private fun setTaskDescription(icon: Bitmap?, @ColorInt colorPrimary: Int) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                    && (icon == null || !icon.isRecycled)
                    && activity != null) {
                activity.setTaskDescription(
                        ActivityManager.TaskDescription(mTeamHelper.toString(), icon, colorPrimary))
            }
        }
    }

    companion object {
        fun newInstance(args: Bundle): ScoutListFragmentBase =
                ActivityScoutListFragment().apply { arguments = args }
    }
}

package com.supercilex.robotscouter.ui.scouting.scoutlist

import android.app.Activity
import android.app.ActivityManager
import android.arch.lifecycle.LiveData
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.support.annotation.ColorInt
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.data.SCOUT_ARGS_KEY
import com.supercilex.robotscouter.util.ui.handleUpNavigation
import com.supercilex.robotscouter.util.ui.isInTabletMode

class ActivityScoutListFragment : ScoutListFragmentBase(), FirebaseAuth.AuthStateListener {
    override val viewHolder: AppBarViewHolderBase by lazy {
        ActivityAppBarViewHolder(dataHolder.teamListener, onScoutingReadyTask.task)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (activity.callingActivity != null && isInTabletMode(context)) {
            activity.setResult(Activity.RESULT_OK, Intent().putExtra(SCOUT_ARGS_KEY, bundle))
            activity.finish()
        }
        FirebaseAuth.getInstance().addAuthStateListener(this)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        val activity = activity as AppCompatActivity
        activity.setSupportActionBar(rootView.findViewById(R.id.toolbar))
        activity.supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        super.onActivityCreated(savedInstanceState)
    }

    override fun onDestroy() {
        super.onDestroy()
        FirebaseAuth.getInstance().removeAuthStateListener(this)
    }

    override fun onOptionsItemSelected(item: MenuItem) = if (item.itemId == android.R.id.home) {
        activity.handleUpNavigation()
        true
    } else {
        super.onOptionsItemSelected(item)
    }

    override fun onAuthStateChanged(auth: FirebaseAuth) {
        if (auth.currentUser == null) onTeamDeleted()
    }

    override fun onTeamDeleted() = activity.finish()

    private inner class ActivityAppBarViewHolder(listener: LiveData<Team>,
                                                 onScoutingReadyTask: Task<Nothing?>) :
            AppBarViewHolderBase(
                    this@ActivityScoutListFragment, rootView, listener, onScoutingReadyTask) {
        override fun bind() {
            super.bind()
            (activity as AppCompatActivity).supportActionBar!!.title = team.toString()
            setTaskDescription(null, ContextCompat.getColor(context, R.color.colorPrimary))
        }

        override fun updateScrim(color: Int, bitmap: Bitmap?) {
            super.updateScrim(color, bitmap)
            header.setStatusBarScrimColor(color)
            setTaskDescription(bitmap, color)
        }

        private fun setTaskDescription(icon: Bitmap?, @ColorInt colorPrimary: Int) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                    && (icon == null || !icon.isRecycled)
                    && activity != null) {
                activity.setTaskDescription(
                        ActivityManager.TaskDescription(team.toString(), icon, colorPrimary))
            }
        }
    }

    companion object {
        fun newInstance(args: Bundle): ScoutListFragmentBase =
                ActivityScoutListFragment().apply { arguments = args }
    }
}

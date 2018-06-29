package com.supercilex.robotscouter.feature.scouts

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
import com.supercilex.robotscouter.core.data.SCOUT_ARGS_KEY
import com.supercilex.robotscouter.core.model.Team
import com.supercilex.robotscouter.core.ui.isInTabletMode
import com.supercilex.robotscouter.shared.handleUpNavigation
import kotlinx.android.synthetic.main.fragment_scout_list.*
import com.supercilex.robotscouter.R as RC

internal class ActivityScoutListFragment : ScoutListFragmentBase(), FirebaseAuth.AuthStateListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activity = requireActivity()
        if (activity.callingActivity != null && activity.isInTabletMode()) {
            activity.setResult(Activity.RESULT_OK, Intent().putExtra(SCOUT_ARGS_KEY, bundle))
            activity.finish()
        }
        FirebaseAuth.getInstance().addAuthStateListener(this)
    }

    override fun newViewModel(savedInstanceState: Bundle?): AppBarViewHolderBase =
            ActivityAppBarViewHolder(
                    savedInstanceState,
                    dataHolder.teamListener,
                    onScoutingReadyTask.task
            )

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        (activity as AppCompatActivity).apply {
            setSupportActionBar(viewHolder.toolbar)
            checkNotNull(supportActionBar).setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        FirebaseAuth.getInstance().removeAuthStateListener(this)
    }

    override fun onOptionsItemSelected(item: MenuItem) = if (item.itemId == android.R.id.home) {
        requireActivity().handleUpNavigation()
        true
    } else {
        super.onOptionsItemSelected(item)
    }

    override fun onAuthStateChanged(auth: FirebaseAuth) {
        if (auth.currentUser == null) onTeamDeleted()
    }

    override fun onTeamDeleted() = requireActivity().finish()

    private inner class ActivityAppBarViewHolder(
            savedInstanceState: Bundle?,
            listener: LiveData<Team?>,
            onScoutingReadyTask: Task<Nothing?>
    ) : AppBarViewHolderBase(
            this@ActivityScoutListFragment,
            savedInstanceState,
            listener,
            onScoutingReadyTask
    ) {
        override fun bind() {
            super.bind()
            checkNotNull((activity as AppCompatActivity).supportActionBar).title = team.toString()
            setTaskDescription(
                    null, ContextCompat.getColor(requireContext(), RC.color.colorPrimary))
        }

        override fun updateScrim(color: Int, bitmap: Bitmap?) {
            super.updateScrim(color, bitmap)
            header.setStatusBarScrimColor(color)
            setTaskDescription(bitmap, color)
        }

        private fun setTaskDescription(icon: Bitmap?, @ColorInt colorPrimary: Int) {
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                (icon == null || !icon.isRecycled) &&
                activity != null
            ) {
                requireActivity().setTaskDescription(
                        ActivityManager.TaskDescription(team.toString(), icon, colorPrimary))
            }
        }
    }

    companion object {
        const val TAG = "ActivityScoutListFrag"

        fun newInstance(args: Bundle): ScoutListFragmentBase =
                ActivityScoutListFragment().apply { arguments = args }
    }
}

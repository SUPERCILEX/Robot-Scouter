package com.supercilex.robotscouter.feature.scouts

import android.app.ActivityManager
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import com.google.firebase.auth.FirebaseAuth
import com.supercilex.robotscouter.core.model.Team
import com.supercilex.robotscouter.core.ui.colorPrimary
import com.supercilex.robotscouter.shared.handleUpNavigation
import kotlinx.android.synthetic.main.fragment_scout_list_toolbar.*

internal class ActivityScoutListFragment : ScoutListFragmentBase(), FirebaseAuth.AuthStateListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseAuth.getInstance().addAuthStateListener(this)
    }

    override fun newViewModel(): AppBarViewHolderBase =
            ActivityAppBarViewHolder(dataHolder.teamListener)

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
            listener: LiveData<Team?>
    ) : AppBarViewHolderBase(this@ActivityScoutListFragment, listener) {
        override fun bind() {
            super.bind()
            checkNotNull((activity as AppCompatActivity).supportActionBar).title = team.toString()
            setTaskDescription(colorPrimary)
        }

        override fun updateScrim(color: Int) {
            super.updateScrim(color)
            header.setStatusBarScrimColor(color)
            setTaskDescription(color)
        }

        private fun setTaskDescription(@ColorInt colorPrimary: Int) {
            if (Build.VERSION.SDK_INT >= 28) {
                activity?.setTaskDescription(
                        ActivityManager.TaskDescription(team.toString(), 0, colorPrimary))
            } else if (Build.VERSION.SDK_INT >= 21) {
                @Suppress("DEPRECATION")
                activity?.setTaskDescription(
                        ActivityManager.TaskDescription(team.toString(), null, colorPrimary))
            }
        }
    }

    companion object {
        const val TAG = "ActivityScoutListFrag"

        fun newInstance(args: Bundle): ScoutListFragmentBase =
                ActivityScoutListFragment().apply { arguments = args }
    }
}

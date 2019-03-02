package com.supercilex.robotscouter.feature.scouts

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.commitNow
import com.supercilex.robotscouter.ActivityViewCreationListener
import com.supercilex.robotscouter.Bridge
import com.supercilex.robotscouter.TabletScoutListFragmentCompanion
import com.supercilex.robotscouter.TeamSelectionListener
import com.supercilex.robotscouter.core.ui.FragmentBase

@Bridge
internal class TabletScoutListContainer : FragmentBase(), ActivityViewCreationListener {
    override fun onActivityViewCreated(
            context: Context,
            listener: TeamSelectionListener
    ) = childFragmentManager.fragments
            .filterIsInstance<ActivityViewCreationListener>()
            .forEach { it.onActivityViewCreated(context, listener) }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.activity_scout_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            childFragmentManager.commitNow {
                add(R.id.scoutList,
                    TabletScoutListFragment.newInstance(requireArguments()),
                    TabletScoutListFragment.TAG)
            }
        }
    }

    companion object : TabletScoutListFragmentCompanion {
        override fun newInstance(args: Bundle) =
                TabletScoutListContainer().apply { arguments = args }
    }
}

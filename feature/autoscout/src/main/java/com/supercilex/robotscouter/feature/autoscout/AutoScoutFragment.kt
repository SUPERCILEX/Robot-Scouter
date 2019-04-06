package com.supercilex.robotscouter.feature.autoscout

import androidx.fragment.app.FragmentManager
import com.supercilex.robotscouter.AutoScoutFragmentCompanion
import com.supercilex.robotscouter.AutoScoutFragmentCompanion.Companion.TAG
import com.supercilex.robotscouter.Bridge
import com.supercilex.robotscouter.core.ui.FragmentBase

@Bridge
internal class AutoScoutFragment : FragmentBase(R.layout.fragment_auto_scout) {
    companion object : AutoScoutFragmentCompanion {
        override fun getInstance(manager: FragmentManager) =
                manager.findFragmentByTag(TAG) as AutoScoutFragment? ?: AutoScoutFragment()
    }
}

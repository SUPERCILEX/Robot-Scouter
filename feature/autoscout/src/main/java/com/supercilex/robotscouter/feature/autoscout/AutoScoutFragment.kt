package com.supercilex.robotscouter.feature.autoscout

import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.supercilex.robotscouter.AutoScoutFragmentCompanion
import com.supercilex.robotscouter.AutoScoutFragmentCompanion.Companion.TAG
import com.supercilex.robotscouter.Bridge
import com.supercilex.robotscouter.core.ui.FragmentBase

@Bridge
internal class AutoScoutFragment : FragmentBase() {
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View = View.inflate(context, R.layout.fragment_auto_scout, null)

    companion object : AutoScoutFragmentCompanion {
        override fun getInstance(manager: FragmentManager) =
                manager.findFragmentByTag(TAG) as AutoScoutFragment? ?: AutoScoutFragment()
    }
}

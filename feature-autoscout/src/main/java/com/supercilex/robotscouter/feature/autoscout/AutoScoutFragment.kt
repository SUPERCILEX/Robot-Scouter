package com.supercilex.robotscouter.feature.autoscout

import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.supercilex.robotscouter.core.data.isFullUser
import com.supercilex.robotscouter.core.data.model.add
import com.supercilex.robotscouter.core.data.model.teamWithSafeDefaults
import com.supercilex.robotscouter.core.data.remote.TeamsAtEventDownloader
import com.supercilex.robotscouter.core.data.teams
import com.supercilex.robotscouter.core.ui.FragmentBase
import kotlinx.android.synthetic.main.fragment_auto_scout.*
import kotlinx.coroutines.experimental.async
import org.jetbrains.anko.support.v4.toast

class AutoScoutFragment : FragmentBase(), View.OnClickListener {
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View = View.inflate(context, R.layout.fragment_auto_scout, null)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!isFullUser) {
            toast("Auth logic unimplemented, please sign in.")
            TODO("Show sign-in card")
        }

        tmpStart.setOnClickListener(this)
        tmpConnect.setOnClickListener(this)
        tmpLoadTeams.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.tmpStart -> AutoScoutService.enslave()
            R.id.tmpConnect -> AutoScoutService.findMaster()
            R.id.tmpLoadTeams -> async {
                val newTeams =
                        TeamsAtEventDownloader.execute(tmpEventKey.text.toString()) ?: return@async
                val numbers = newTeams.map { it.number }.toMutableList()
                numbers.removeAll(teams.map { it.number })
                for (number in numbers) {
                    teamWithSafeDefaults(number, "").add()
                }
            }
        }
    }

    companion object {
        const val TAG = "AutoScoutFragment"

        fun getInstance(manager: FragmentManager) =
                manager.findFragmentByTag(TAG) as AutoScoutFragment? ?: AutoScoutFragment()
    }
}

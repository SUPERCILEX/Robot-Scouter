package com.supercilex.robotscouter.feature.scouts

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.fragment.app.transaction
import com.supercilex.robotscouter.Bridge
import com.supercilex.robotscouter.ScoutListActivityCompanion
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.data.SCOUT_ARGS_KEY
import com.supercilex.robotscouter.core.ui.ActivityBase
import com.supercilex.robotscouter.core.ui.addNewDocumentFlags
import com.supercilex.robotscouter.core.unsafeLazy
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.multipleTask
import com.supercilex.robotscouter.R as RC

@Bridge
internal class ScoutListActivity : ActivityBase() {
    private val scoutListFragment by unsafeLazy {
        supportFragmentManager.findFragmentByTag(ActivityScoutListFragment.TAG) as ActivityScoutListFragment
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(RC.style.RobotScouter_NoActionBar_TransparentStatusBar)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scout_list)
        if (savedInstanceState == null) {
            supportFragmentManager.transaction {
                add(R.id.scoutList,
                    ActivityScoutListFragment.newInstance(intent.getBundleExtra(SCOUT_ARGS_KEY)),
                    ActivityScoutListFragment.TAG)
            }
        }
    }

    override fun onShortcut(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_N -> if (event.isShiftPressed) {
                scoutListFragment.addScoutWithSelector()
            } else {
                scoutListFragment.addScout()
            }
            KeyEvent.KEYCODE_D -> scoutListFragment.showTeamDetails()
            else -> return false
        }
        return true
    }

    companion object : ScoutListActivityCompanion {
        override fun createIntent(args: Bundle): Intent =
                RobotScouter.intentFor<ScoutListActivity>(SCOUT_ARGS_KEY to args)
                        .multipleTask()
                        .addNewDocumentFlags()
    }
}

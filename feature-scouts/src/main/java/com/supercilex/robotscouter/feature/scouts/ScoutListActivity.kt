package com.supercilex.robotscouter.feature.scouts

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.data.SCOUT_ARGS_KEY
import com.supercilex.robotscouter.core.ui.ActivityBase
import com.supercilex.robotscouter.core.ui.KeyboardShortcutHandler
import com.supercilex.robotscouter.core.ui.addNewDocumentFlags
import com.supercilex.robotscouter.core.unsafeLazy
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.multipleTask

class ScoutListActivity : ActivityBase() {
    override val keyboardShortcutHandler = object : KeyboardShortcutHandler() {
        override fun onFilteredKeyUp(keyCode: Int, event: KeyEvent) {
            when (keyCode) {
                KeyEvent.KEYCODE_N -> if (event.isShiftPressed) {
                    scoutListFragment.addScoutWithSelector()
                } else {
                    scoutListFragment.addScout()
                }
                KeyEvent.KEYCODE_D -> scoutListFragment.showTeamDetails()
            }
        }
    }
    private val scoutListFragment: ActivityScoutListFragment by unsafeLazy {
        supportFragmentManager.findFragmentByTag(ActivityScoutListFragment.TAG) as ActivityScoutListFragment
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.RobotScouter_NoActionBar_TransparentStatusBar)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scout_list)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .add(R.id.scoutList,
                         ActivityScoutListFragment.newInstance(intent.getBundleExtra(SCOUT_ARGS_KEY)),
                         ActivityScoutListFragment.TAG)
                    .commit()
        }
    }

    companion object {
        fun createIntent(args: Bundle): Intent =
                RobotScouter.intentFor<ScoutListActivity>(SCOUT_ARGS_KEY to args)
                        .multipleTask()
                        .addNewDocumentFlags()
    }
}

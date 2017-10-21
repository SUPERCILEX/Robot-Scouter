package com.supercilex.robotscouter.ui.scouting.scoutlist

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.util.data.SCOUT_ARGS_KEY
import com.supercilex.robotscouter.util.ui.ActivityBase
import com.supercilex.robotscouter.util.ui.KeyboardShortcutHandler
import com.supercilex.robotscouter.util.ui.addNewDocumentFlags
import com.supercilex.robotscouter.util.unsafeLazy

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
                    .add(R.id.scout_list,
                         ActivityScoutListFragment.newInstance(intent.getBundleExtra(SCOUT_ARGS_KEY)),
                         ActivityScoutListFragment.TAG)
                    .commit()
        }
    }

    companion object {
        fun createIntent(args: Bundle): Intent =
                Intent(RobotScouter.INSTANCE, ScoutListActivity::class.java)
                        .putExtra(SCOUT_ARGS_KEY, args)
                        .addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                        .addNewDocumentFlags()
    }
}

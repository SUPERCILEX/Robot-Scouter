package com.supercilex.robotscouter.feature.scouts

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.commit
import com.supercilex.robotscouter.Bridge
import com.supercilex.robotscouter.ScoutListActivityCompanion
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.data.SCOUT_ARGS_KEY
import com.supercilex.robotscouter.core.ui.ActivityBase
import com.supercilex.robotscouter.core.ui.addNewDocumentFlags
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.multipleTask
import com.supercilex.robotscouter.R as RC

@Bridge
internal class ScoutListActivity : ActivityBase() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(RC.style.RobotScouter_NoActionBar_TransparentStatusBar)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scout_list)
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                add(R.id.scoutList,
                    ActivityScoutListFragment.newInstance(intent.getBundleExtra(SCOUT_ARGS_KEY)),
                    ActivityScoutListFragment.TAG)
            }
        }
    }

    companion object : ScoutListActivityCompanion {
        override fun createIntent(args: Bundle): Intent =
                RobotScouter.intentFor<ScoutListActivity>(SCOUT_ARGS_KEY to args)
                        .putExtra("android.intent.extra.shortcut.SHELF_GROUP_ID", "scouts")
                        .multipleTask()
                        .addNewDocumentFlags()
    }
}

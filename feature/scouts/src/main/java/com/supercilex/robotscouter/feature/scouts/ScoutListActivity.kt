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
import com.supercilex.robotscouter.R as RC

@Bridge
internal class ScoutListActivity : ActivityBase() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(RC.style.RobotScouter_NoActionBar_TransparentStatusBar)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.scout_list_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                add(R.id.scout_list,
                    ActivityScoutListFragment.newInstance(
                            checkNotNull(intent.getBundleExtra(SCOUT_ARGS_KEY))),
                    ActivityScoutListFragment.TAG)
            }
        }
    }

    companion object : ScoutListActivityCompanion {
        override fun createIntent(args: Bundle): Intent =
                Intent(RobotScouter, ScoutListActivity::class.java)
                        .putExtra(SCOUT_ARGS_KEY, args)
                        .putExtra("android.intent.extra.shortcut.SHELF_GROUP_ID", "scouts")
                        .addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                        .addNewDocumentFlags()
    }
}

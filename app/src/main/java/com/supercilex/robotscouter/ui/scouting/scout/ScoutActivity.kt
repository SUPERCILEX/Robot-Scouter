package com.supercilex.robotscouter.ui.scouting.scout

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.util.data.SCOUT_ARGS_KEY
import com.supercilex.robotscouter.util.ui.addNewDocumentFlags

class ScoutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.RobotScouter_NoActionBar_TransparentStatusBar)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scout)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .add(R.id.scout_list, ActivityScoutListFragment.newInstance(
                            intent.getBundleExtra(SCOUT_ARGS_KEY)))
                    .commit()
        }
    }

    companion object {
        fun createIntent(context: Context, args: Bundle): Intent =
                Intent(context, ScoutActivity::class.java)
                        .putExtra(SCOUT_ARGS_KEY, args)
                        .addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                        .addNewDocumentFlags()
    }
}

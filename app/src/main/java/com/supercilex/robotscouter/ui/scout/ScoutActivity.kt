package com.supercilex.robotscouter.ui.scout

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity

import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Team

import com.supercilex.robotscouter.ui.scout.ScoutListFragmentBase.KEY_SCOUT_ARGS

class ScoutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.RobotScouter_NoActionBar_TransparentStatusBar)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scout)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .add(R.id.scouts, ActivityScoutListFragment.newInstance(
                            intent.getBundleExtra(KEY_SCOUT_ARGS)))
                    .commit()
        }
    }

    companion object {
        fun start(context: Context, team: Team, addScout: Boolean) = context.startActivity(
                createIntent(context, ScoutListFragmentBase.getBundle(team, addScout, null)))

        fun createIntent(context: Context, args: Bundle): Intent {
            val starter = Intent(context, ScoutActivity::class.java).putExtra(KEY_SCOUT_ARGS, args)

            starter.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                starter.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                starter.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
            }

            return starter
        }
    }
}

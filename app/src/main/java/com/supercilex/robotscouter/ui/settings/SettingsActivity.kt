package com.supercilex.robotscouter.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.NavUtils
import android.support.v4.app.TaskStackBuilder
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.ui.teamlist.TeamListActivity

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.RobotScouter_Settings)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
    }

    override fun onOptionsItemSelected(item: MenuItem) = if (item.itemId == android.R.id.home) {
        if (NavUtils.shouldUpRecreateTask(this, Intent(this, TeamListActivity::class.java))) {
            TaskStackBuilder.create(this).addParentStack(this).startActivities()
        }
        finish()
        true
    } else {
        super.onOptionsItemSelected(item)
    }

    companion object {
        fun show(context: Context) =
                context.startActivity(Intent(context, SettingsActivity::class.java))
    }
}

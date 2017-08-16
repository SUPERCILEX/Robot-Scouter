package com.supercilex.robotscouter.ui.scouting.templatelist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.NavUtils
import android.support.v4.app.TaskStackBuilder
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.ui.teamlist.TeamListActivity
import com.supercilex.robotscouter.util.data.TEAM_KEY
import com.supercilex.robotscouter.util.data.getTeamExtra
import com.supercilex.robotscouter.util.data.putExtra

class TemplateListActivity : AppCompatActivity() {
    private val templateListFragment: TemplateListFragment by lazy {
        supportFragmentManager.findFragmentByTag(TemplateListFragment.TAG) as TemplateListFragment
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.RobotScouter_NoActionBar)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_template_list)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .add(R.id.template_list,
                         TemplateListFragment.newInstance(
                                 if (intent.hasExtra(TEAM_KEY)) intent.getTeamExtra() else null),
                         TemplateListFragment.TAG)
                    .commit()
        }
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

    override fun onBackPressed() {
        if (!templateListFragment.onBackPressed()) super.onBackPressed()
    }

    companion object {
        fun start(context: Context, team: Team? = null) =
                context.startActivity(Intent(context, TemplateListActivity::class.java)
                                              .apply { team?.let { putExtra(it) } })
    }
}

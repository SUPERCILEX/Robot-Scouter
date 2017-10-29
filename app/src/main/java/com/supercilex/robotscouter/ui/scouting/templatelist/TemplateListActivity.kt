package com.supercilex.robotscouter.ui.scouting.templatelist

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.util.data.getTabId
import com.supercilex.robotscouter.util.data.getTabIdBundle
import com.supercilex.robotscouter.util.ui.handleUpNavigation
import com.supercilex.robotscouter.util.unsafeLazy
import org.jetbrains.anko.intentFor

class TemplateListActivity : AppCompatActivity() {
    private val templateListFragment: TemplateListFragment by unsafeLazy {
        supportFragmentManager.findFragmentByTag(TemplateListFragment.TAG) as TemplateListFragment
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.RobotScouter_NoActionBar)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_template_list)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .add(R.id.template_list,
                         TemplateListFragment.newInstance(getTabId(intent.extras)),
                         TemplateListFragment.TAG)
                    .commit()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        templateListFragment.handleArgs(intent.extras)
    }

    override fun onOptionsItemSelected(item: MenuItem) = if (item.itemId == android.R.id.home) {
        handleUpNavigation()
        true
    } else {
        super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (!templateListFragment.onBackPressed()) super.onBackPressed()
    }

    companion object {
        fun createIntent(templateId: String? = null): Intent =
                RobotScouter.INSTANCE.intentFor<TemplateListActivity>()
                        .putExtras(getTabIdBundle(templateId))
    }
}

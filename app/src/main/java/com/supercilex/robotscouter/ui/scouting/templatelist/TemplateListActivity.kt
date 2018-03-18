package com.supercilex.robotscouter.ui.scouting.templatelist

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.util.data.getTabId
import com.supercilex.robotscouter.util.data.getTabIdBundle
import com.supercilex.robotscouter.util.ui.ActivityBase
import com.supercilex.robotscouter.util.ui.handleUpNavigation
import com.supercilex.robotscouter.util.unsafeLazy
import org.jetbrains.anko.intentFor

class TemplateListActivity : ActivityBase() {
    private val templateListFragment: TemplateListFragment by unsafeLazy {
        supportFragmentManager.findFragmentByTag(TemplateListFragment.TAG) as TemplateListFragment
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.RobotScouter_NoActionBar)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_template_list)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .add(R.id.templateList,
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
                RobotScouter.intentFor<TemplateListActivity>().putExtras(getTabIdBundle(templateId))
    }
}

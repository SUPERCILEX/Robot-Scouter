package com.supercilex.robotscouter.feature.templates

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.transaction
import com.supercilex.robotscouter.Bridge
import com.supercilex.robotscouter.TemplateListActivityCompanion
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.data.getTabId
import com.supercilex.robotscouter.core.data.getTabIdBundle
import com.supercilex.robotscouter.core.ui.ActivityBase
import com.supercilex.robotscouter.core.unsafeLazy
import com.supercilex.robotscouter.shared.handleUpNavigation
import org.jetbrains.anko.intentFor
import com.supercilex.robotscouter.R as RC

@Bridge
class TemplateListActivity : ActivityBase() {
    private val templateListFragment: TemplateListFragment by unsafeLazy {
        supportFragmentManager.findFragmentByTag(TemplateListFragment.TAG) as TemplateListFragment
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(RC.style.RobotScouter_NoActionBar)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_template_list)
        if (savedInstanceState == null) {
            supportFragmentManager.transaction {
                add(R.id.templateList,
                    TemplateListFragment.newInstance(getTabId(intent.extras)),
                    TemplateListFragment.TAG)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        templateListFragment.handleArgs(checkNotNull(intent.extras))
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

    companion object : TemplateListActivityCompanion {
        override fun createIntent(templateId: String?): Intent =
                RobotScouter.intentFor<TemplateListActivity>().putExtras(getTabIdBundle(templateId))
    }
}

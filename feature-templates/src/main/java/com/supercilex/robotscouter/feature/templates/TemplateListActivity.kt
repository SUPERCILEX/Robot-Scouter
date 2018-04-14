package com.supercilex.robotscouter.feature.templates

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import com.supercilex.robotscouter.core.ui.ActivityBase
import com.supercilex.robotscouter.core.unsafeLazy
import com.supercilex.robotscouter.shared.handleUpNavigation
import com.supercilex.robotscouter.shared.templateIntent

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
                         TemplateListFragment.newInstance(intent.data.pathSegments.last()),
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
        fun createIntent(templateId: String? = null) = templateIntent(templateId)
    }
}

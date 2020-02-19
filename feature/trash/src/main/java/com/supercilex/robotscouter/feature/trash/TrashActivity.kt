package com.supercilex.robotscouter.feature.trash

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.commit
import com.supercilex.robotscouter.Bridge
import com.supercilex.robotscouter.TrashActivityCompanion
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.ui.ActivityBase
import com.supercilex.robotscouter.R as RC

@Bridge
internal class TrashActivity : ActivityBase() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(RC.style.RobotScouter_NoActionBar)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.trash_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                add(R.id.trash, TrashFragment.newInstance(), TrashFragment.TAG)
            }
        }
    }

    companion object : TrashActivityCompanion {
        override fun createIntent(): Intent = Intent(RobotScouter, TrashActivity::class.java)
    }
}

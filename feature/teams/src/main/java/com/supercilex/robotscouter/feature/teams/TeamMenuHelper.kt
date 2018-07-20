package com.supercilex.robotscouter.feature.teams

import android.animation.ValueAnimator
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.children
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.selection.SelectionTracker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.supercilex.robotscouter.DrawerToggler
import com.supercilex.robotscouter.TeamExporter
import com.supercilex.robotscouter.common.isSingleton
import com.supercilex.robotscouter.core.data.isFullUser
import com.supercilex.robotscouter.core.ui.animateColorChange
import com.supercilex.robotscouter.core.unsafeLazy
import com.supercilex.robotscouter.shared.TeamDetailsDialog
import com.supercilex.robotscouter.shared.TeamSharer
import org.jetbrains.anko.find
import java.util.Locale
import com.supercilex.robotscouter.R as RC

internal class TeamMenuHelper(
        private val fragment: TeamListFragment,
        private val tracker: SelectionTracker<String>
) : AllChangesSelectionObserver<String>(), View.OnClickListener {
    private val activity = fragment.requireActivity() as AppCompatActivity

    private val fab by unsafeLazy { activity.find<FloatingActionButton>(RC.id.fab) }
    private val drawerLayout by unsafeLazy { activity.find<DrawerLayout>(RC.id.drawerLayout) }
    private val toolbar by unsafeLazy {
        activity.find<Toolbar>(RC.id.toolbar).apply {
            setNavigationOnClickListener(this@TeamMenuHelper)
        }
    }

    private var isMenuReady = false
    private val teamMenuItems = mutableListOf<MenuItem>()
    private val teamsMenuItems = mutableListOf<MenuItem>()
    private val normalMenuItems = mutableListOf<MenuItem>()

    override fun onSelectionChanged() {
        val selection = tracker.selection
        onNormalMenuChanged(selection.isEmpty)
        onTeamMenuChanged(selection.isSingleton)
        onTeamsMenuChanged(!selection.isEmpty)

        updateToolbarTitle()
    }

    override fun onClick(view: View) {
        if (tracker.hasSelection()) {
            tracker.clearSelection()
        } else {
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.team_options, menu)

        teamMenuItems.clear()
        teamMenuItems.addAll(menu.children.filter { teamMenuIds.contains(it.itemId) })
        teamsMenuItems.clear()
        teamsMenuItems.addAll(menu.children.filter { teamsMenuIds.contains(it.itemId) })

        normalMenuItems.clear()
        normalMenuItems.addAll(menu.children.filterNot { menuIds.contains(it.itemId) })

        isMenuReady = true

        if (tracker.hasSelection()) {
            onNormalMenuChanged(false)
            onTeamMenuChanged(tracker.selection.isSingleton)
            onTeamsMenuChanged(true)

            updateToolbarTitle()
        }
    }

    fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_export_teams -> (activity as TeamExporter).export()
            R.id.action_share -> if (TeamSharer.shareTeams(fragment, fragment.selectedTeams)) {
                tracker.clearSelection()
            }
            R.id.action_edit_team_details -> TeamDetailsDialog.show(
                    fragment.childFragmentManager, fragment.selectedTeams.first())
            R.id.action_delete -> DeleteTeamDialog.show(
                    fragment.childFragmentManager, fragment.selectedTeams)
            else -> return false
        }
        return true
    }

    private fun onNormalMenuChanged(visible: Boolean) {
        // Post twice: 1. for speed, 2. so the items update when restoring from a config change
        val updateItems = {
            for (item in normalMenuItems) {
                item.isVisible =
                        visible && if (item.itemId == RC.id.action_sign_in) !isFullUser else true
            }
        }
        updateItems()
        toolbar.post(updateItems)

        if (visible) {
            fab.show()
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNDEFINED)
        } else {
            fab.hide()
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        }

        setupIcon(visible)
        updateToolbarColor(visible)
    }

    private fun onTeamMenuChanged(visible: Boolean) {
        for (item in teamMenuItems) item.isVisible = visible
    }

    private fun onTeamsMenuChanged(visible: Boolean) {
        for (item in teamsMenuItems) item.isVisible = visible
    }

    private fun updateToolbarTitle() {
        val selection = tracker.selection
        checkNotNull(activity.supportActionBar).title = if (selection.isEmpty) {
            activity.getString(RC.string.app_name)
        } else {
            String.format(Locale.getDefault(), "%d", selection.size())
        }
    }

    private fun setupIcon(visible: Boolean) {
        val toggler = activity as DrawerToggler
        if (visible) {
            toggler.toggle(true)
        } else {
            checkNotNull(activity.supportActionBar).apply {
                // Replace hamburger icon with back button
                setDisplayHomeAsUpEnabled(false)
                toggler.toggle(false)
                setDisplayHomeAsUpEnabled(true)
            }
        }
    }

    private fun updateToolbarColor(visible: Boolean) {
        fun Drawable?.shouldUpdateBackground(newColor: Int): Boolean = this !is ColorDrawable ||
                color != ContextCompat.getColor(activity, newColor)

        @ColorRes val oldColorPrimary =
                if (visible) RC.color.selected_toolbar else RC.color.colorPrimary
        @ColorRes val newColorPrimary =
                if (visible) RC.color.colorPrimary else RC.color.selected_toolbar

        if (toolbar.background.shouldUpdateBackground(newColorPrimary)) {
            animateColorChange(
                    oldColorPrimary, newColorPrimary, ValueAnimator.AnimatorUpdateListener {
                toolbar.setBackgroundColor(it.animatedValue as Int)
            })
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            @ColorRes val oldColorPrimaryDark =
                    if (visible) RC.color.selected_status_bar else RC.color.colorPrimaryDark
            @ColorRes val newColorPrimaryDark =
                    if (visible) RC.color.colorPrimaryDark else RC.color.selected_status_bar

            if (
                drawerLayout.statusBarBackgroundDrawable.shouldUpdateBackground(newColorPrimaryDark)
            ) animateColorChange(
                    oldColorPrimaryDark,
                    newColorPrimaryDark,
                    ValueAnimator.AnimatorUpdateListener {
                        drawerLayout.setStatusBarBackgroundColor(it.animatedValue as Int)
                    }
            )
        }
    }

    private companion object {
        val teamMenuIds = listOf(R.id.action_edit_team_details)
        val teamsMenuIds = listOf(
                R.id.action_export_teams,
                R.id.action_share,
                R.id.action_delete
        )

        val menuIds = teamMenuIds + teamsMenuIds
    }
}

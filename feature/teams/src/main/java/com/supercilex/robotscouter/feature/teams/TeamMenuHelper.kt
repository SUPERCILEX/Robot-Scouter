package com.supercilex.robotscouter.feature.teams

import android.animation.ValueAnimator
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.support.annotation.ColorRes
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.supercilex.robotscouter.DrawerToggler
import com.supercilex.robotscouter.TeamExporter
import com.supercilex.robotscouter.core.data.isFullUser
import com.supercilex.robotscouter.core.data.isSingleton
import com.supercilex.robotscouter.core.data.teams
import com.supercilex.robotscouter.core.model.Team
import com.supercilex.robotscouter.core.ui.OnBackPressedListener
import com.supercilex.robotscouter.core.ui.animateColorChange
import com.supercilex.robotscouter.core.ui.notifyItemsNoChangeAnimation
import com.supercilex.robotscouter.core.unsafeLazy
import com.supercilex.robotscouter.shared.TeamDetailsDialog
import com.supercilex.robotscouter.shared.TeamSharer
import org.jetbrains.anko.find
import java.util.Locale
import com.supercilex.robotscouter.R as RC

internal class TeamMenuHelper(
        private val fragment: TeamListFragment,
        private val recyclerView: RecyclerView,
        selectedTeams: List<Team> = emptyList()
) : View.OnClickListener, OnBackPressedListener {
    private val activity = fragment.activity as AppCompatActivity

    private var _selectedTeams = selectedTeams.toMutableList()
    val selectedTeams: List<Team> get() = _selectedTeams
    lateinit var adapter: FirestoreRecyclerAdapter<Team, TeamViewHolder>

    private val fab by unsafeLazy { activity.find<FloatingActionButton>(R.id.fab) }
    private val drawerLayout by unsafeLazy { activity.find<DrawerLayout>(RC.id.drawerLayout) }
    private val toolbar by unsafeLazy {
        activity.find<Toolbar>(RC.id.toolbar).apply {
            setNavigationOnClickListener(this@TeamMenuHelper)
        }
    }

    private var isMenuReady = false

    private lateinit var signInItem: MenuItem

    private lateinit var exportItem: MenuItem
    private lateinit var shareItem: MenuItem
    private lateinit var editTeamDetailsItem: MenuItem
    private lateinit var deleteItem: MenuItem

    private var selectAllSnackBar = snackBar()

    init {
        if (selectedTeams.isNotEmpty()) onRestore()
    }

    fun resetToolbarWithSave() {
        val prev = _selectedTeams

        _selectedTeams = mutableListOf()
        setNormalMenuItemsVisible(true)
        setContextMenuItemsVisible(false)
        setTeamSpecificItemsVisible(false)
        updateToolbarTitle()
        selectAllSnackBar.dismiss()
        // Generate a new SnackBar since a user dismissed one can't be shown again.
        selectAllSnackBar = snackBar()
        _selectedTeams = prev
    }

    override fun onClick(view: View) = if (selectedTeams.isEmpty()) {
        drawerLayout.openDrawer(GravityCompat.START)
    } else {
        resetMenu()
    }

    fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.team_options, menu)

        signInItem = menu.findItem(RC.id.action_sign_in)

        exportItem = menu.findItem(R.id.action_export_teams)
        shareItem = menu.findItem(R.id.action_share)
        editTeamDetailsItem = menu.findItem(R.id.action_edit_team_details)
        deleteItem = menu.findItem(R.id.action_delete)

        isMenuReady = true
        updateState()
    }

    fun onOptionsItemSelected(item: MenuItem): Boolean {
        val team = selectedTeams.first()
        when (item.itemId) {
            R.id.action_export_teams -> (activity as TeamExporter).export()
            R.id.action_share -> if (TeamSharer.shareTeams(fragment, selectedTeams)) {
                resetMenu()
            }
            R.id.action_edit_team_details ->
                TeamDetailsDialog.show(fragment.childFragmentManager, team)
            R.id.action_delete ->
                DeleteTeamDialog.show(fragment.childFragmentManager, selectedTeams)
            else -> return false
        }
        return true
    }

    override fun onBackPressed() = if (selectedTeams.isEmpty()) {
        false
    } else {
        resetMenu()
        true
    }

    fun saveState(outState: Bundle) {
        outState.putParcelableArray(SELECTED_TEAMS_KEY, selectedTeams.toTypedArray())
    }

    fun restoreState(savedInstanceState: Bundle?) {
        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_TEAMS_KEY)) {
            _selectedTeams = savedInstanceState.getParcelableArray(SELECTED_TEAMS_KEY).map {
                it as Team
            }.toMutableList()
            onRestore()
        }

        if (isMenuReady) updateState()
    }

    fun onTeamContextMenuRequested(team: Team) {
        val hadNormalMenu = selectedTeams.isEmpty()

        if (selectedTeams.find { it.id == team.id } == null) {
            _selectedTeams.add(team)
        } else {
            _selectedTeams.removeAll { it.id == team.id }
        }

        updateToolbarTitle()

        if (hadNormalMenu) {
            updateState()
            notifyItemsChanged()
        } else {
            val newSize = selectedTeams.size
            when (newSize) {
                0 -> resetMenu()
                1 -> setTeamSpecificItemsVisible(true)
                adapter.itemCount -> selectAllSnackBar.dismiss()
                else -> selectAllSnackBar.show()
            }
            if (newSize > 1) setTeamSpecificItemsVisible(false)
        }
    }

    fun onSelectedTeamChanged(oldTeam: Team, team: Team) {
        _selectedTeams.remove(oldTeam)
        _selectedTeams.add(team)
    }

    fun onSelectedTeamRemoved(oldTeam: Team) {
        _selectedTeams.remove(oldTeam)
        if (selectedTeams.isEmpty()) {
            resetMenu()
        } else if (isMenuReady) {
            updateState()
        }
    }

    private fun setContextMenuItemsVisible(visible: Boolean) {
        exportItem.isVisible = visible
        shareItem.isVisible = visible
        deleteItem.isVisible = visible
    }

    private fun setTeamSpecificItemsVisible(visible: Boolean) {
        editTeamDetailsItem.isVisible = visible
        if (visible) selectAllSnackBar.dismiss()
    }

    private fun setNormalMenuItemsVisible(visible: Boolean) {
        // Post twice: 1. for speed, 2. so the items update on config change
        val updateItems = { signInItem.isVisible = visible && !isFullUser }
        updateItems()
        toolbar.post(updateItems)

        if (visible) {
            fab.show()
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNDEFINED)
            updateToolbarTitle()
        } else {
            fab.hide()
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        }

        setupIcon(visible)
        updateToolbarColor(visible)
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

            if (drawerLayout.statusBarBackgroundDrawable
                    .shouldUpdateBackground(newColorPrimaryDark)) {
                animateColorChange(
                        oldColorPrimaryDark,
                        newColorPrimaryDark,
                        ValueAnimator.AnimatorUpdateListener {
                            drawerLayout.setStatusBarBackgroundColor(it.animatedValue as Int)
                        }
                )
            }
        }
    }

    private fun updateToolbarTitle() {
        checkNotNull(activity.supportActionBar).title = if (selectedTeams.isEmpty()) {
            activity.getString(RC.string.app_name)
        } else {
            String.format(Locale.getDefault(), "%d", selectedTeams.size)
        }
    }

    private fun snackBar(): Snackbar = Snackbar.make(
            checkNotNull(fragment.view),
            R.string.team_multiple_selected_message,
            Snackbar.LENGTH_INDEFINITE
    ).setAction(R.string.team_select_all_title) {
        _selectedTeams = adapter.snapshots.toMutableList()
        updateState()
        notifyItemsChanged()
    }

    private fun updateState() {
        if (selectedTeams.isNotEmpty()) {
            setNormalMenuItemsVisible(false)
            setContextMenuItemsVisible(true)
            setTeamSpecificItemsVisible(selectedTeams.isSingleton)
            updateToolbarTitle()
        }
    }

    private fun resetMenu() {
        _selectedTeams = mutableListOf()
        resetToolbarWithSave()
        notifyItemsChanged()
    }

    private fun onRestore() {
        if (selectedTeams.size in 2 until teams.size) selectAllSnackBar.show()
        fab.post { notifyItemsChanged() }
    }

    private fun notifyItemsChanged() {
        recyclerView.notifyItemsNoChangeAnimation()
    }

    private companion object {
        const val SELECTED_TEAMS_KEY = "selected_teams_key"
    }
}

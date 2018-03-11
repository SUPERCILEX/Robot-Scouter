package com.supercilex.robotscouter.ui.teamlist

import android.animation.ValueAnimator
import android.arch.lifecycle.DefaultLifecycleObserver
import android.arch.lifecycle.LifecycleOwner
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
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.ui.TeamDetailsDialog
import com.supercilex.robotscouter.ui.TeamSharer
import com.supercilex.robotscouter.util.isFullUser
import com.supercilex.robotscouter.util.isSingleton
import com.supercilex.robotscouter.util.ui.OnBackPressedListener
import com.supercilex.robotscouter.util.ui.animateColorChange
import com.supercilex.robotscouter.util.ui.mainHandler
import com.supercilex.robotscouter.util.ui.notifyItemsNoChangeAnimation
import org.jetbrains.anko.find
import java.util.Locale

class TeamMenuHelper(
        private val fragment: TeamListFragment,
        private val recyclerView: RecyclerView
) : View.OnClickListener, OnBackPressedListener {
    private val activity = fragment.activity as AppCompatActivity

    private var _selectedTeams = mutableListOf<Team>()
    val selectedTeams: List<Team> get() = _selectedTeams
    lateinit var adapter: FirestoreRecyclerAdapter<Team, TeamViewHolder>

    private val fab: FloatingActionButton = activity.find(R.id.fab)
    private val drawerLayout: DrawerLayout = activity.find(R.id.drawer_layout)
    private val toolbar: Toolbar = activity.find<Toolbar>(R.id.toolbar).apply {
        setNavigationOnClickListener(this@TeamMenuHelper)
    }

    private var isMenuReady: Boolean = false

    private lateinit var signInItem: MenuItem

    private lateinit var exportItem: MenuItem
    private lateinit var shareItem: MenuItem
    private lateinit var editTeamDetailsItem: MenuItem
    private lateinit var deleteItem: MenuItem

    private var selectAllSnackBar = snackBar()

    private fun snackBar(): Snackbar = Snackbar.make(
            fragment.view!!,
            R.string.team_multiple_selected_message,
            Snackbar.LENGTH_INDEFINITE
    ).setAction(R.string.team_select_all_title) {
        _selectedTeams = adapter.snapshots.toMutableList()
        updateState()
        notifyItemsChanged()
    }

    override fun onClick(view: View) = if (selectedTeams.isEmpty()) {
        drawerLayout.openDrawer(GravityCompat.START)
    } else {
        resetMenu()
    }

    fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.team_options, menu)

        signInItem = menu.findItem(R.id.action_sign_in)

        exportItem = menu.findItem(R.id.action_export_teams)
        shareItem = menu.findItem(R.id.action_share)
        editTeamDetailsItem = menu.findItem(R.id.action_edit_team_details)
        deleteItem = menu.findItem(R.id.action_delete)

        isMenuReady = true
        updateState()
    }

    private fun updateState() {
        if (selectedTeams.isNotEmpty()) {
            setNormalMenuItemsVisible(false)
            setContextMenuItemsVisible(true)
            setTeamSpecificItemsVisible(selectedTeams.isSingleton)
            updateToolbarTitle()
        }
    }

    fun resetMenu() {
        _selectedTeams = mutableListOf()
        setNormalMenuItemsVisible(true)
        setContextMenuItemsVisible(false)
        setTeamSpecificItemsVisible(false)
        updateToolbarTitle()
        selectAllSnackBar.dismiss()
        // Generate a new SnackBar since a user dismissed one can't be shown again.
        selectAllSnackBar = snackBar()
        notifyItemsChanged()
    }

    fun onOptionsItemSelected(item: MenuItem): Boolean {
        val team = selectedTeams.first()
        when (item.itemId) {
            R.id.action_export_teams -> fragment.exportTeams()
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
            notifyItemsChanged()

            fragment.lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    if (selectedTeams.size in 1 until adapter.itemCount) {
                        selectAllSnackBar.show()
                    }
                    fragment.lifecycle.removeObserver(this)
                }
            })
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
        } else {
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
        mainHandler.post(updateItems)

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
        val toggle = (activity as TeamListActivity).drawerToggle
        if (visible) {
            toggle.isDrawerIndicatorEnabled = true
        } else {
            activity.supportActionBar!!.apply {
                // Replace hamburger icon with back button
                setDisplayHomeAsUpEnabled(false)
                toggle.isDrawerIndicatorEnabled = false
                setDisplayHomeAsUpEnabled(true)
            }
        }
    }

    private fun updateToolbarColor(visible: Boolean) {
        fun Drawable?.shouldUpdateBackground(newColor: Int): Boolean = this !is ColorDrawable
                || color != ContextCompat.getColor(activity, newColor)

        @ColorRes val oldColorPrimary =
                if (visible) R.color.selected_toolbar else R.color.colorPrimary
        @ColorRes val newColorPrimary =
                if (visible) R.color.colorPrimary else R.color.selected_toolbar

        if (toolbar.background.shouldUpdateBackground(newColorPrimary)) {
            animateColorChange(
                    oldColorPrimary, newColorPrimary, ValueAnimator.AnimatorUpdateListener {
                toolbar.setBackgroundColor(it.animatedValue as Int)
            })
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            @ColorRes val oldColorPrimaryDark =
                    if (visible) R.color.selected_status_bar else R.color.colorPrimaryDark
            @ColorRes val newColorPrimaryDark =
                    if (visible) R.color.colorPrimaryDark else R.color.selected_status_bar

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
        activity.supportActionBar!!.title = if (selectedTeams.isEmpty()) {
            activity.getString(R.string.app_name)
        } else {
            String.format(Locale.getDefault(), "%d", selectedTeams.size)
        }
    }

    private fun notifyItemsChanged() {
        recyclerView.notifyItemsNoChangeAnimation()
    }

    private companion object {
        const val SELECTED_TEAMS_KEY = "selected_teams_key"
    }
}

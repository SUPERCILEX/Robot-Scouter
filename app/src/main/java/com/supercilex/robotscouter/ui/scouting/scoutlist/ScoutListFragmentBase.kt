package com.supercilex.robotscouter.ui.scouting.scoutlist

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.appindexing.FirebaseUserActions
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.ui.ShouldUploadMediaToTbaDialog
import com.supercilex.robotscouter.ui.TeamDetailsDialog
import com.supercilex.robotscouter.ui.TeamSharer
import com.supercilex.robotscouter.ui.scouting.templatelist.TemplateListActivity
import com.supercilex.robotscouter.util.data.KEY_ADD_SCOUT
import com.supercilex.robotscouter.util.data.KEY_OVERRIDE_TEMPLATE_KEY
import com.supercilex.robotscouter.util.data.getScoutBundle
import com.supercilex.robotscouter.util.data.getTabKey
import com.supercilex.robotscouter.util.data.model.TeamHolder
import com.supercilex.robotscouter.util.data.model.addScout
import com.supercilex.robotscouter.util.data.model.viewAction
import com.supercilex.robotscouter.util.data.model.visitTbaWebsite
import com.supercilex.robotscouter.util.data.model.visitTeamWebsite
import com.supercilex.robotscouter.util.isOffline
import com.supercilex.robotscouter.util.ui.FragmentBase
import com.supercilex.robotscouter.util.ui.TeamMediaCreator
import com.supercilex.robotscouter.util.ui.TemplateSelectionListener

abstract class ScoutListFragmentBase : FragmentBase(),
        TemplateSelectionListener, Observer<Team>, TeamMediaCreator.StartCaptureListener {
    protected val rootView: View by lazy {
        View.inflate(context, R.layout.fragment_scout_list, null)
    }
    protected abstract val viewHolder: AppBarViewHolderBase

    protected val dataHolder: TeamHolder by lazy { ViewModelProviders.of(this).get(TeamHolder::class.java) }
    private lateinit var team: Team
    private var pagerAdapter: ScoutPagerAdapter? = null

    protected var onScoutingReadyTask = TaskCompletionSource<Nothing?>()
    private var savedState: Bundle? = null

    private val scoutKey: String? get() {
        var scoutKey: String? = pagerAdapter?.currentTabKey
        if (scoutKey == null && savedState != null) scoutKey = getTabKey(savedState!!)
        if (scoutKey == null) scoutKey = getTabKey(arguments)
        return scoutKey
    }
    protected val bundle: Bundle
        get() = getScoutBundle(team, arguments.getBoolean(KEY_ADD_SCOUT), scoutKey)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        savedState = savedInstanceState

        dataHolder.init(savedInstanceState ?: arguments)
        team = dataHolder.teamListener.value!!
        dataHolder.teamListener.observe(this, this)
    }

    override fun onChanged(team: Team?) {
        if (team == null) onTeamDeleted()
        else {
            this.team = team
            if (!onScoutingReadyTask.task.isComplete) {
                initScoutList()
                onScoutingReadyTask.setResult(null)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        if (savedState == null && isOffline()) {
            Snackbar.make(rootView, R.string.offline_reassurance, Snackbar.LENGTH_LONG).show()
        }
        return rootView
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewHolder
        if (savedInstanceState != null) viewHolder.restoreState(savedInstanceState)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) = viewHolder.initMenu()

    override fun onStart() {
        super.onStart()
        FirebaseUserActions.getInstance().start(team.viewAction)
    }

    override fun onStop() {
        super.onStop()
        FirebaseUserActions.getInstance().end(team.viewAction)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        pagerAdapter?.onSaveInstanceState(outState)
        dataHolder.onSaveInstanceState(outState)
        viewHolder.onSaveInstanceState(outState)
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) =
            viewHolder.onRequestPermissionsResult(requestCode, permissions, grantResults)

    override fun onStartCapture(shouldUploadMediaToTba: Boolean) =
            viewHolder.onStartCapture(shouldUploadMediaToTba)

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) =
            viewHolder.onActivityResult(requestCode, resultCode)

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_new_scout -> pagerAdapter!!.currentTabKey = team.addScout()
            R.id.action_add_media -> ShouldUploadMediaToTbaDialog.show(this)
            R.id.action_share -> TeamSharer.shareTeams(activity, listOf(team))
            R.id.action_visit_tba_website -> team.visitTbaWebsite(context)
            R.id.action_visit_team_website -> team.visitTeamWebsite(context)
            R.id.action_edit_template -> TemplateListActivity.start(context, team)
            R.id.action_edit_team_details -> TeamDetailsDialog.show(childFragmentManager, team)
            else -> return false
        }
        return true
    }

    override fun onTemplateSelected(key: String) {
        pagerAdapter!!.currentTabKey = team.addScout(key)
    }

    private fun initScoutList() {
        val tabLayout = rootView.findViewById<TabLayout>(R.id.tabs)
        val viewPager = rootView.findViewById<ViewPager>(R.id.viewpager)
        pagerAdapter = ScoutPagerAdapter(this, tabLayout, team)
        pagerAdapter!!.currentTabKey = scoutKey

        viewPager.adapter = pagerAdapter
        tabLayout.setupWithViewPager(viewPager)

        if (arguments.getBoolean(KEY_ADD_SCOUT, false)) {
            arguments.remove(KEY_ADD_SCOUT)
            pagerAdapter!!.currentTabKey =
                    team.addScout(arguments.getString(KEY_OVERRIDE_TEMPLATE_KEY, null))
        }
    }

    protected abstract fun onTeamDeleted()
}

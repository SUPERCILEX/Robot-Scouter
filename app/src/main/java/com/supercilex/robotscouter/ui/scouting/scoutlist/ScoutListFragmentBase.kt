package com.supercilex.robotscouter.ui.scouting.scoutlist

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.appindexing.FirebaseUserActions
import com.google.firebase.firestore.QuerySnapshot
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.data.model.TemplateType
import com.supercilex.robotscouter.ui.ShouldUploadMediaToTbaDialog
import com.supercilex.robotscouter.ui.TeamDetailsDialog
import com.supercilex.robotscouter.ui.TeamSharer
import com.supercilex.robotscouter.ui.scouting.templatelist.TemplateListActivity
import com.supercilex.robotscouter.util.AsyncTaskExecutor
import com.supercilex.robotscouter.util.CrashLogger
import com.supercilex.robotscouter.util.data.KEY_ADD_SCOUT
import com.supercilex.robotscouter.util.data.KEY_OVERRIDE_TEMPLATE_KEY
import com.supercilex.robotscouter.util.data.SCOUT_PARSER
import com.supercilex.robotscouter.util.data.getScoutBundle
import com.supercilex.robotscouter.util.data.getTabId
import com.supercilex.robotscouter.util.data.model.TeamHolder
import com.supercilex.robotscouter.util.data.model.addScout
import com.supercilex.robotscouter.util.data.model.getTemplatesQuery
import com.supercilex.robotscouter.util.data.model.visitTbaWebsite
import com.supercilex.robotscouter.util.data.model.visitTeamWebsite
import com.supercilex.robotscouter.util.data.viewAction
import com.supercilex.robotscouter.util.isOffline
import com.supercilex.robotscouter.util.ui.FragmentBase
import com.supercilex.robotscouter.util.ui.RecyclerPoolHolder
import com.supercilex.robotscouter.util.ui.TeamMediaCreator
import com.supercilex.robotscouter.util.ui.TemplateSelectionListener

abstract class ScoutListFragmentBase : FragmentBase(), RecyclerPoolHolder,
        TemplateSelectionListener, Observer<Team>, TeamMediaCreator.StartCaptureListener {
    override val recyclerPool = RecyclerView.RecycledViewPool()

    protected val rootView: View by lazy {
        View.inflate(context, R.layout.fragment_scout_list, null)
    }
    protected abstract val viewHolder: AppBarViewHolderBase

    protected val dataHolder: TeamHolder by lazy { ViewModelProviders.of(this).get(TeamHolder::class.java) }
    private lateinit var team: Team
    private var pagerAdapter: ScoutPagerAdapter? = null

    protected var onScoutingReadyTask = TaskCompletionSource<Nothing?>()
    private var savedState: Bundle? = null

    private val scoutId: String? get() {
        var scoutId: String? = pagerAdapter?.currentTabId
        if (scoutId == null && savedState != null) scoutId = getTabId(savedState!!)
        if (scoutId == null) scoutId = getTabId(arguments)
        return scoutId
    }
    protected val bundle: Bundle
        get() = getScoutBundle(team, arguments.getBoolean(KEY_ADD_SCOUT), scoutId = scoutId)

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
            R.id.action_new_scout -> addScout()
            R.id.action_add_media -> ShouldUploadMediaToTbaDialog.show(this)
            R.id.action_share -> TeamSharer.shareTeams(activity, listOf(team))
            R.id.action_visit_tba_website -> team.visitTbaWebsite(context)
            R.id.action_visit_team_website -> team.visitTeamWebsite(context)
            R.id.action_edit_template -> {
                val templateId = team.templateId

                TemplateType.coerce(templateId)?.let {
                    startActivity(TemplateListActivity.createIntent(it.id.toString()))
                    return true
                }

                getTemplatesQuery().get().continueWith(
                        AsyncTaskExecutor, Continuation<QuerySnapshot, Boolean> {
                    it.result.map { SCOUT_PARSER.parseSnapshot(it) }
                            .find { it.id == templateId } != null
                }).addOnSuccessListener { ownsTemplate ->
                    if (ownsTemplate) {
                        startActivity(TemplateListActivity.createIntent(templateId))
                    } else {
                        Toast.makeText(context,
                                       R.string.error_template_access_denied,
                                       Toast.LENGTH_LONG)
                                .show()
                    }
                }.addOnFailureListener(CrashLogger)
            }
            R.id.action_edit_team_details -> showTeamDetails()
            else -> return false
        }
        return true
    }

    fun showTeamDetails() {
        TeamDetailsDialog.show(childFragmentManager, team)
    }

    fun addScoutWithSelector() {
        ScoutTemplateSelectorDialog.show(childFragmentManager)
    }

    fun addScout(id: String? = null) {
        pagerAdapter!!.currentTabId = team.addScout(id)
    }

    override fun onTemplateSelected(id: String) = addScout(id)

    private fun initScoutList() {
        val tabLayout = rootView.findViewById<TabLayout>(R.id.tabs)
        val viewPager = rootView.findViewById<ViewPager>(R.id.viewpager)
        pagerAdapter = ScoutPagerAdapter(this, tabLayout, team)
        pagerAdapter!!.currentTabId = scoutId

        viewPager.adapter = pagerAdapter
        tabLayout.setupWithViewPager(viewPager)

        if (arguments.getBoolean(KEY_ADD_SCOUT, false)) {
            arguments.remove(KEY_ADD_SCOUT)
            addScout(arguments.getString(KEY_OVERRIDE_TEMPLATE_KEY, null))
        }
    }

    protected abstract fun onTeamDeleted()
}

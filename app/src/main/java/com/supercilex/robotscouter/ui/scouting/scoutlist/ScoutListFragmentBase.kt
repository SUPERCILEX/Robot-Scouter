package com.supercilex.robotscouter.ui.scouting.scoutlist

import android.arch.lifecycle.DefaultLifecycleObserver
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
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
import com.supercilex.robotscouter.util.data.KEY_ADD_SCOUT
import com.supercilex.robotscouter.util.data.KEY_OVERRIDE_TEMPLATE_KEY
import com.supercilex.robotscouter.util.data.asLiveData
import com.supercilex.robotscouter.util.data.getScoutBundle
import com.supercilex.robotscouter.util.data.getTabId
import com.supercilex.robotscouter.util.data.model.TeamHolder
import com.supercilex.robotscouter.util.data.model.addScout
import com.supercilex.robotscouter.util.data.model.getTemplatesQuery
import com.supercilex.robotscouter.util.data.model.visitTbaWebsite
import com.supercilex.robotscouter.util.data.model.visitTeamWebsite
import com.supercilex.robotscouter.util.data.scoutParser
import com.supercilex.robotscouter.util.data.viewAction
import com.supercilex.robotscouter.util.isOffline
import com.supercilex.robotscouter.util.logFailures
import com.supercilex.robotscouter.util.ui.FragmentBase
import com.supercilex.robotscouter.util.ui.RecyclerPoolHolder
import com.supercilex.robotscouter.util.ui.TeamMediaCreator
import com.supercilex.robotscouter.util.ui.TemplateSelectionListener
import com.supercilex.robotscouter.util.unsafeLazy
import com.supercilex.robotscouter.util.user
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.support.v4.find
import org.jetbrains.anko.support.v4.longToast

abstract class ScoutListFragmentBase : FragmentBase(), RecyclerPoolHolder,
        TemplateSelectionListener, Observer<Team>, TeamMediaCreator.StartCaptureListener {
    override val recyclerPool = RecyclerView.RecycledViewPool()

    protected abstract val viewHolder: AppBarViewHolderBase

    protected val dataHolder: TeamHolder by unsafeLazy {
        ViewModelProviders.of(this).get(TeamHolder::class.java)
    }
    private lateinit var team: Team
    // It's not a lateinit because it could be used before initialization
    var pagerAdapter: ScoutPagerAdapter? = null

    protected var onScoutingReadyTask = TaskCompletionSource<Nothing?>()
    private var savedState: Bundle? = null

    private val scoutId: String?
        get() {
            var scoutId: String? = pagerAdapter?.currentTabId
            if (scoutId == null && savedState != null) scoutId = getTabId(savedState!!)
            if (scoutId == null) scoutId = getTabId(arguments)
            return scoutId
        }
    protected val bundle: Bundle
        get() = getScoutBundle(team, arguments!!.getBoolean(KEY_ADD_SCOUT), scoutId = scoutId)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        savedState = savedInstanceState

        dataHolder.init(savedInstanceState ?: arguments!!)
        team = dataHolder.teamListener.value!!
        dataHolder.teamListener.observe(this, this)
    }

    override fun onChanged(team: Team?) {
        if (team == null) {
            onTeamDeleted()
        } else {
            this.team = team
            if (!onScoutingReadyTask.task.isComplete) {
                initScoutList()
                onScoutingReadyTask.setResult(null)
            }
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View = View.inflate(context, R.layout.fragment_scout_list, null)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (savedInstanceState == null && isOffline()) {
            longSnackbar(view, R.string.scout_offline_rationale)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewHolder // Force initialize
        if (savedInstanceState != null) viewHolder.restoreState(savedInstanceState)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                owner.lifecycle.removeObserver(this)
                viewHolder.initMenu()
            }
        })
    }

    override fun onStart() {
        super.onStart()
        FirebaseUserActions.getInstance().start(team.viewAction).logFailures()
        // Don't add an auth listener since it could get called after onStop which won't work if we
        // try to remove a fragment.
        if (user == null) onTeamDeleted()
    }

    override fun onStop() {
        super.onStop()
        FirebaseUserActions.getInstance().end(team.viewAction).logFailures()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        pagerAdapter?.onSaveInstanceState(outState)
        dataHolder.onSaveInstanceState(outState)
        viewHolder.onSaveInstanceState(outState)
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
    ) = viewHolder.onRequestPermissionsResult(requestCode, permissions, grantResults)

    override fun onStartCapture(shouldUploadMediaToTba: Boolean) =
            viewHolder.onStartCapture(shouldUploadMediaToTba)

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) =
            viewHolder.onActivityResult(requestCode, resultCode)

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val activity = activity!!
        when (item.itemId) {
            R.id.action_new_scout -> addScout()
            R.id.action_add_media -> ShouldUploadMediaToTbaDialog.show(this)
            R.id.action_share -> TeamSharer.shareTeams(activity, listOf(team))
            R.id.action_visit_tba_website -> team.visitTbaWebsite(activity)
            R.id.action_visit_team_website -> team.visitTeamWebsite(activity)
            R.id.action_edit_template -> {
                val templateId = team.templateId

                TemplateType.coerce(templateId)?.let {
                    startActivity(TemplateListActivity.createIntent(it.id.toString()))
                    return true
                }

                getTemplatesQuery().get().continueWith(
                        AsyncTaskExecutor, Continuation<QuerySnapshot, Boolean> {
                    it.result.map { scoutParser.parseSnapshot(it) }
                            .find { it.id == templateId } != null
                }).addOnSuccessListener(activity) { ownsTemplate ->
                    if (ownsTemplate) {
                        startActivity(TemplateListActivity.createIntent(templateId))
                    } else {
                        longToast(R.string.template_access_denied_error)
                    }
                }.logFailures()
            }
            R.id.action_edit_team_details -> showTeamDetails()
            else -> return false
        }
        return true
    }

    fun showTeamDetails() = TeamDetailsDialog.show(childFragmentManager, team)

    fun addScoutWithSelector() = ScoutTemplateSelectorDialog.show(childFragmentManager)

    fun addScout(id: String? = null) {
        pagerAdapter!!.apply {
            currentTabId = team.addScout(id, holder.scouts.asLiveData())
        }
    }

    override fun onTemplateSelected(id: String) = addScout(id)

    private fun initScoutList() {
        val tabLayout = find<TabLayout>(R.id.tabs)
        val viewPager = find<ViewPager>(R.id.viewpager)
        pagerAdapter = ScoutPagerAdapter(this, tabLayout, team)
        pagerAdapter!!.currentTabId = scoutId

        viewPager.adapter = pagerAdapter
        tabLayout.setupWithViewPager(viewPager)

        arguments!!.let {
            if (it.getBoolean(KEY_ADD_SCOUT, false)) {
                it.remove(KEY_ADD_SCOUT)
                addScout(it.getString(KEY_OVERRIDE_TEMPLATE_KEY, null))
            }
        }
    }

    protected abstract fun onTeamDeleted()
}

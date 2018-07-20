package com.supercilex.robotscouter.feature.scouts

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.get
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.material.tabs.TabLayout
import com.google.firebase.appindexing.FirebaseUserActions
import com.supercilex.robotscouter.core.data.KEY_ADD_SCOUT
import com.supercilex.robotscouter.core.data.KEY_OVERRIDE_TEMPLATE_KEY
import com.supercilex.robotscouter.core.data.getScoutBundle
import com.supercilex.robotscouter.core.data.getTabId
import com.supercilex.robotscouter.core.data.getTeam
import com.supercilex.robotscouter.core.data.getTemplateLink
import com.supercilex.robotscouter.core.data.model.TeamHolder
import com.supercilex.robotscouter.core.data.model.addScout
import com.supercilex.robotscouter.core.data.model.ownsTemplateTask
import com.supercilex.robotscouter.core.data.toBundle
import com.supercilex.robotscouter.core.data.viewAction
import com.supercilex.robotscouter.core.isOffline
import com.supercilex.robotscouter.core.logFailures
import com.supercilex.robotscouter.core.model.Team
import com.supercilex.robotscouter.core.model.TemplateType
import com.supercilex.robotscouter.core.ui.FragmentBase
import com.supercilex.robotscouter.core.ui.RecyclerPoolHolder
import com.supercilex.robotscouter.core.ui.TemplateSelectionListener
import com.supercilex.robotscouter.core.ui.find
import com.supercilex.robotscouter.core.ui.longSnackbar
import com.supercilex.robotscouter.core.unsafeLazy
import com.supercilex.robotscouter.shared.CaptureTeamMediaListener
import com.supercilex.robotscouter.shared.ShouldUploadMediaToTbaDialog
import com.supercilex.robotscouter.shared.TeamDetailsDialog
import com.supercilex.robotscouter.shared.TeamSharer
import kotlinx.android.synthetic.main.fragment_scout_list.*
import com.supercilex.robotscouter.R as RC

internal abstract class ScoutListFragmentBase : FragmentBase(), RecyclerPoolHolder,
        TemplateSelectionListener, Observer<Team?>, CaptureTeamMediaListener {
    override val recyclerPool = RecyclerView.RecycledViewPool()

    protected lateinit var viewHolder: AppBarViewHolderBase
        private set

    protected val dataHolder: TeamHolder by unsafeLazy {
        ViewModelProviders.of(this).get<TeamHolder>()
    }
    private lateinit var team: Team
    // It's not a lateinit because it could be used before initialization
    var pagerAdapter: ScoutPagerAdapter? = null

    protected var onScoutingReadyTask = TaskCompletionSource<Nothing?>()
    private var savedState: Bundle? = null

    private val tabs by unsafeLazy { find<TabLayout>(RC.id.tabs) }

    private val scoutId: String?
        get() {
            val savedState = savedState
            var scoutId: String? = pagerAdapter?.currentTabId
            if (scoutId == null && savedState != null) scoutId = getTabId(savedState)
            if (scoutId == null) scoutId = getTabId(arguments)
            return scoutId
        }
    protected val bundle: Bundle
        get() = getScoutBundle(
                team, checkNotNull(arguments).getBoolean(KEY_ADD_SCOUT), scoutId = scoutId)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        savedState = savedInstanceState

        val state = savedInstanceState ?: checkNotNull(arguments)
        dataHolder.init(state)
        team = state.getTeam()
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
        if (savedInstanceState == null && isOffline) {
            longSnackbar(view, R.string.scout_offline_rationale)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewHolder = newViewModel(savedInstanceState)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        viewHolder.initMenu()
    }

    override fun onStart() {
        super.onStart()
        FirebaseUserActions.getInstance().start(team.viewAction).logFailures()
    }

    override fun onStop() {
        super.onStop()
        FirebaseUserActions.getInstance().end(team.viewAction).logFailures()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        pagerAdapter?.onSaveInstanceState(outState)
        viewHolder.onSaveInstanceState(outState)
        outState.putAll(dataHolder.teamListener.value?.toBundle() ?: Bundle.EMPTY)
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
    ) = viewHolder.onRequestPermissionsResult(requestCode, permissions, grantResults)

    override fun startCapture(shouldUploadMediaToTba: Boolean) =
            viewHolder.startCapture(shouldUploadMediaToTba)

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) =
            viewHolder.onActivityResult(requestCode, resultCode, data)

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_new_scout -> addScout()
            R.id.action_add_media -> ShouldUploadMediaToTbaDialog.show(this)
            R.id.action_share -> TeamSharer.shareTeams(this, listOf(team))
            R.id.action_edit_template -> {
                val templateId = team.templateId
                val intent: Intent = Intent(Intent.ACTION_VIEW)
                        .setData(getTemplateLink(templateId).toUri())

                TemplateType.coerce(templateId)?.let {
                    startActivity(intent)
                    return true
                }

                ownsTemplateTask(templateId).logFailures().addOnSuccessListener(requireActivity()) {
                    if (it) {
                        startActivity(intent)
                    } else {
                        longSnackbar(find(R.id.root), R.string.scout_template_access_denied_error)
                    }
                }
            }
            R.id.action_edit_team_details -> showTeamDetails()
            else -> return false
        }
        return true
    }

    fun showTeamDetails() = TeamDetailsDialog.show(childFragmentManager, team)

    fun addScoutWithSelector() = ScoutTemplateSelectorDialog.show(childFragmentManager)

    fun addScout(id: String? = null) {
        checkNotNull(pagerAdapter).apply {
            currentTabId = team.addScout(id, holder.scouts)
        }
    }

    override fun onTemplateSelected(id: String) = addScout(id)

    private fun initScoutList() {
        pagerAdapter = ScoutPagerAdapter(this, team)
        checkNotNull(pagerAdapter).currentTabId = scoutId

        viewPager.adapter = pagerAdapter
        tabs.setupWithViewPager(viewPager)

        checkNotNull(arguments).let {
            if (it.getBoolean(KEY_ADD_SCOUT, false)) {
                it.remove(KEY_ADD_SCOUT)
                addScout(it.getString(KEY_OVERRIDE_TEMPLATE_KEY, null))
            }
        }
    }

    protected abstract fun newViewModel(savedInstanceState: Bundle?): AppBarViewHolderBase

    protected abstract fun onTeamDeleted()
}

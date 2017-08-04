package com.supercilex.robotscouter.ui.teamlist

import android.arch.lifecycle.LifecycleFragment
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.ListPreloader
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.util.ViewPreloadSizeProvider
import com.firebase.ui.database.ChangeEventListener
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.ObservableSnapshotArray
import com.google.firebase.crash.FirebaseCrash
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.ui.CardListHelper
import com.supercilex.robotscouter.util.ui.getAdapterItems
import java.util.Collections

class TeamListAdapter(snapshots: ObservableSnapshotArray<Team>,
                      private val fragment: LifecycleFragment,
                      private val menuManager: TeamMenuHelper,
                      private val selectedTeamKeyListener: LiveData<String?>) :
        FirebaseRecyclerAdapter<Team, TeamViewHolder>(
                snapshots, R.layout.team_list_row_layout, TeamViewHolder::class.java, fragment),
        ListPreloader.PreloadModelProvider<Team>, Observer<String?> {
    private val viewSizeProvider = ViewPreloadSizeProvider<Team>()
    private val preloader = RecyclerViewPreloader<Team>(
            Glide.with(fragment),
            this,
            viewSizeProvider,
            5)

    private val recyclerView = fragment.view!!.findViewById<RecyclerView>(R.id.list)
    private val cardListHelper = CardListHelper(this, recyclerView)
    private val noTeamsHint: View = fragment.view!!.findViewById(R.id.no_content_hint)

    private var selectedTeamKey: String? = null

    init {
        recyclerView.addOnScrollListener(preloader)
        selectedTeamKeyListener.observeForever(this)
    }

    override fun onChanged(teamKey: String?) {
        val oldKey = selectedTeamKey
        selectedTeamKey = teamKey

        for (i in 0 until itemCount) {
            val key = getItem(i).key
            if (TextUtils.equals(key, selectedTeamKey) || TextUtils.equals(key, oldKey)) {
                notifyItemChanged(i)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): TeamViewHolder =
            super.onCreateViewHolder(parent, viewType).also {
                viewSizeProvider.setView(it.mediaImageView)
            }

    public override fun populateViewHolder(teamHolder: TeamViewHolder, team: Team, position: Int) {
        cardListHelper.onBind(teamHolder)
        teamHolder.bind(
                team,
                fragment,
                recyclerView,
                menuManager,
                isTeamSelected(team),
                menuManager.areTeamsSelected(),
                TextUtils.equals(selectedTeamKey, team.key))
    }

    private fun isTeamSelected(team: Team) = menuManager.selectedTeams.contains(team)

    override fun getPreloadRequestBuilder(team: Team): RequestBuilder<*> =
            TeamViewHolder.getTeamMediaRequestBuilder(isTeamSelected(team), fragment.context, team)

    override fun getPreloadItems(position: Int): List<Team> = Collections.singletonList(getItem(position))

    override fun onChildChanged(type: ChangeEventListener.EventType,
                                snapshot: DataSnapshot,
                                index: Int,
                                oldIndex: Int) {
        super.onChildChanged(type, snapshot, index, oldIndex)
        cardListHelper.onChildChanged(type, index)
        if (type == ChangeEventListener.EventType.CHANGED) {
            for (oldTeam in menuManager.selectedTeams) {
                val team = getItem(index)
                if (TextUtils.equals(oldTeam.key, team.key)) {
                    menuManager.onSelectedTeamChanged(oldTeam, team)
                    break
                }
            }
        } else if (type == ChangeEventListener.EventType.REMOVED && menuManager.areTeamsSelected()) {
            val tmpTeams = getAdapterItems(this)
            for (oldTeam in menuManager.selectedTeams) {
                if (!tmpTeams.contains(oldTeam)) { // We found the deleted item
                    menuManager.onSelectedTeamRemoved(oldTeam)
                    break
                }
            }
        }
    }

    override fun onDataChanged() {
        noTeamsHint.visibility = if (itemCount == 0) View.VISIBLE else View.GONE
    }

    override fun cleanup() {
        super.cleanup()
        onDataChanged()
        recyclerView.removeOnScrollListener(preloader)
        selectedTeamKeyListener.removeObserver(this)
    }

    override fun onCancelled(error: DatabaseError) = FirebaseCrash.report(error.toException())
}

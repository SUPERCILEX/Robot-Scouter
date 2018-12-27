package com.supercilex.robotscouter.feature.teams

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.distinctUntilChanged
import androidx.recyclerview.selection.SelectionTracker
import com.bumptech.glide.Glide
import com.bumptech.glide.ListPreloader
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.util.ViewPreloadSizeProvider
import com.firebase.ui.common.ChangeEventType
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.DocumentSnapshot
import com.supercilex.robotscouter.core.LateinitVal
import com.supercilex.robotscouter.core.data.teams
import com.supercilex.robotscouter.core.model.Team
import com.supercilex.robotscouter.core.ui.SavedStateAdapter
import com.supercilex.robotscouter.shared.CardListHelper
import kotlinx.android.synthetic.main.team_list_row_layout.*
import org.jetbrains.anko.support.v4.find
import java.util.Collections

internal class TeamListAdapter(
        savedInstanceState: Bundle?,
        private val fragment: Fragment,
        private val selectedTeamIdListener: MutableLiveData<Team?>
) : SavedStateAdapter<Team, TeamViewHolder>(
        FirestoreRecyclerOptions.Builder<Team>()
                .setSnapshotArray(teams)
                .setLifecycleOwner(fragment.viewLifecycleOwner)
                .build(),
        savedInstanceState,
        fragment.find(R.id.teamsView)
), ListPreloader.PreloadModelProvider<Team>, Observer<Team?> {
    var selectionTracker: SelectionTracker<String> by LateinitVal()

    private val viewSizeProvider = ViewPreloadSizeProvider<Team>()
    private val preloader = RecyclerViewPreloader<Team>(
            Glide.with(fragment),
            this,
            viewSizeProvider,
            5
    )

    private val cardListHelper = CardListHelper(this, recyclerView)

    private val distinctSelectedTeamIdListener = selectedTeamIdListener.distinctUntilChanged()
    private var selectedTeamId: String? = null
    private var hasSelectedTeamChanged = false

    init {
        recyclerView.addOnScrollListener(preloader)
        distinctSelectedTeamIdListener.observeForever(this)
    }

    override fun startListening() {
        super.startListening()

        // More annoying constructor bugs: this will be called before we can assign our fields,
        // thus the NPE
        @Suppress("UNNECESSARY_SAFE_CALL")
        distinctSelectedTeamIdListener?.observeForever(this)
    }

    override fun onChanged(team: Team?) {
        val oldId = selectedTeamId
        val newId = team?.id.also { selectedTeamId = it }
        if (oldId == newId) return
        hasSelectedTeamChanged = newId != null

        fun notify(id: String?) {
            if (id != null) {
                snapshots.indexOfFirst { it.id == id }.let {
                    if (it != -1) notifyItemChanged(it)
                }
            }
        }

        notify(oldId)
        notify(newId)
    }

    fun startScroll() {
        if (hasSelectedTeamChanged) {
            snapshots.indexOfFirst { it.id == selectedTeamId }.let {
                if (it != -1) recyclerView.smoothScrollToPosition(it)
            }
        }
        hasSelectedTeamChanged = false
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeamViewHolder =
            TeamViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                            R.layout.team_list_row_layout,
                            parent,
                            false
                    ),
                    fragment
            ).also {
                viewSizeProvider.setView(it.media)
            }

    override fun onBindViewHolder(teamHolder: TeamViewHolder, position: Int, team: Team) {
        cardListHelper.onBind(teamHolder, position)
        teamHolder.bind(
                team,
                team.isSelected(),
                selectionTracker.hasSelection(),
                selectedTeamId == team.id
        )
    }

    override fun getPreloadRequestBuilder(team: Team): RequestBuilder<*> =
            TeamViewHolder.getTeamMediaRequestBuilder(
                    team.isSelected(), fragment.requireContext(), team)

    private fun Team.isSelected() = selectionTracker.isSelected(id)

    override fun getPreloadItems(position: Int): List<Team> =
            Collections.singletonList(getItem(position))

    override fun onChildChanged(
            type: ChangeEventType,
            snapshot: DocumentSnapshot,
            newIndex: Int,
            oldIndex: Int
    ) {
        super.onChildChanged(type, snapshot, newIndex, oldIndex)
        cardListHelper.onChildChanged(type, oldIndex)

        if (type == ChangeEventType.REMOVED) {
            val id = snapshot.id

            if (selectedTeamIdListener.value?.id == id) {
                selectedTeamIdListener.value = null
            }

            if (selectionTracker.isSelected(id)) {
                selectionTracker.setItemsSelected(listOf(id), false)
            }
        }
    }

    override fun stopListening() {
        super.stopListening()
        recyclerView.removeOnScrollListener(preloader)
        distinctSelectedTeamIdListener.removeObserver(this)
    }
}

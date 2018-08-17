package com.supercilex.robotscouter.feature.teams

import android.view.MotionEvent
import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.widget.RecyclerView
import com.supercilex.robotscouter.core.data.nullOrFull
import com.supercilex.robotscouter.core.ui.ItemDetailsBase
import com.supercilex.robotscouter.core.ui.ItemDetailsLookupBase
import kotlinx.android.synthetic.main.team_list_row_layout.*

internal class TeamKeyProvider(
        private val adapter: TeamListAdapter
) : ItemKeyProvider<String>(SCOPE_MAPPED) {
    override fun getKey(position: Int) = adapter.getItem(position).id

    override fun getPosition(key: String) = adapter.snapshots.indexOfFirst { it.id == key }
}

internal class TeamDetailsLookup(recyclerView: RecyclerView) :
        ItemDetailsLookupBase<String, TeamViewHolder, TeamDetails>(recyclerView, ::TeamDetails)

internal class TeamDetails(
        private val holder: TeamViewHolder
) : ItemDetailsBase<String, TeamViewHolder>(holder) {
    override fun getSelectionKey() = holder.team.id.nullOrFull()

    override fun inSelectionHotspot(e: MotionEvent): Boolean {
        val media = holder.media
        return e.rawX in media.left..media.right
    }
}

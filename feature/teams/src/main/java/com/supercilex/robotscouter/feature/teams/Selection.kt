package com.supercilex.robotscouter.feature.teams

import android.view.MotionEvent
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.widget.RecyclerView
import com.supercilex.robotscouter.core.data.nullOrFull
import kotlinx.android.synthetic.main.team_list_row_layout.*

internal class TeamKeyProvider(
        private val adapter: TeamListAdapter
) : ItemKeyProvider<String>(SCOPE_MAPPED) {
    override fun getKey(position: Int) = adapter.getItem(position).id

    override fun getPosition(key: String) = adapter.snapshots.indexOfFirst { it.id == key }
}

internal class TeamDetailsLookup(
        private val recyclerView: RecyclerView
) : ItemDetailsLookup<String>() {
    override fun getItemDetails(e: MotionEvent) = recyclerView.findChildViewUnder(e.x, e.y)?.let {
        recyclerView.getChildViewHolder(it) as? TeamViewHolder
    }?.let(::TeamDetails)
}

internal class TeamDetails(
        private val holder: TeamViewHolder
) : ItemDetailsLookup.ItemDetails<String>() {
    override fun getPosition() = holder.adapterPosition

    override fun getSelectionKey() = holder.team.id.nullOrFull()

    override fun inSelectionHotspot(e: MotionEvent): Boolean {
        val media = holder.media
        return e.rawX in media.left..media.right
    }
}

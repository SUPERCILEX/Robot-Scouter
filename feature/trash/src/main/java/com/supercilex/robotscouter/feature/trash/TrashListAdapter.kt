package com.supercilex.robotscouter.feature.trash

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.supercilex.robotscouter.core.LateinitVal

internal class TrashListAdapter : ListAdapter<Trash, TrashViewHolder>(differ) {
    var selectionTracker: SelectionTracker<String> by LateinitVal()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = TrashViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.trash_item, parent, false)
    )

    override fun onBindViewHolder(holder: TrashViewHolder, position: Int) {
        val trash = getItem(position)
        holder.bind(trash, selectionTracker.isSelected(trash.id))
    }

    public override fun getItem(position: Int): Trash = super.getItem(position)

    private companion object {
        val differ = object : DiffUtil.ItemCallback<Trash>() {
            override fun areItemsTheSame(oldItem: Trash, newItem: Trash) = oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Trash, newItem: Trash) = oldItem == newItem
        }
    }
}

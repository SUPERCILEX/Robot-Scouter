package com.supercilex.robotscouter.ui.teamlist

import android.graphics.drawable.Drawable
import android.support.annotation.Keep
import android.support.v4.app.Fragment
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.ui.TeamDetailsDialog
import com.supercilex.robotscouter.ui.scout.ScoutListFragmentBase
import com.supercilex.robotscouter.util.animateCircularReveal

private val RecyclerView.isScrolling get() = scrollState != RecyclerView.SCROLL_STATE_IDLE

class TeamViewHolder @Keep constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener {
    private val scrollStateListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) =
                setProgressVisibility()
    }
    private val mediaLoadProgressListener = object : RequestListener<Drawable> {
        override fun onResourceReady(resource: Drawable?,
                                     model: Any?,
                                     target: Target<Drawable>,
                                     dataSource: DataSource,
                                     isFirstResource: Boolean): Boolean {
            isMediaLoaded = true
            return false
        }

        override fun onLoadFailed(e: GlideException?,
                                  model: Any?,
                                  target: Target<Drawable>,
                                  isFirstResource: Boolean): Boolean {
            isMediaLoaded = true
            return false
        }
    }

    private val mediaImageView: ImageView = itemView.findViewById(R.id.media)
    private val mediaLoadProgress: ProgressBar = itemView.findViewById(R.id.progress)
    private val numberTextView: TextView = itemView.findViewById(R.id.number)
    private val nameTextView: TextView = itemView.findViewById(R.id.name)
    private val newScoutButton: ImageButton = itemView.findViewById(R.id.new_scout)

    private lateinit var team: Team
    private lateinit var fragment: Fragment
    private lateinit var recyclerView: RecyclerView
    private lateinit var menuManager: TeamMenuManager
    private var isItemSelected: Boolean = false
    private var couldItemBeSelected: Boolean = false
    private var isScouting: Boolean = false

    private var isMediaLoaded: Boolean = false
        set(value) {
            field = value
            setProgressVisibility(field)
        }

    fun bind(team: Team,
             fragment: Fragment,
             recyclerView: RecyclerView,
             menuManager: TeamMenuManager,
             isItemSelected: Boolean,
             couldItemBeSelected: Boolean,
             isScouting: Boolean) {
        this.team = team
        this.fragment = fragment
        this.recyclerView = recyclerView
        this.menuManager = menuManager
        this.isItemSelected = isItemSelected
        this.couldItemBeSelected = couldItemBeSelected
        this.isScouting = isScouting

        setTeamNumber()
        setTeamName()
        updateItemStatus()
        recyclerView.removeOnScrollListener(scrollStateListener)
        recyclerView.addOnScrollListener(scrollStateListener)

        mediaImageView.setOnClickListener(this)
        mediaImageView.setOnLongClickListener(this)
        newScoutButton.setOnClickListener(this)
        itemView.setOnClickListener(this)
        itemView.setOnLongClickListener(this)
    }

    private fun updateItemStatus() {
        isMediaLoaded = isItemSelected
        setProgressVisibility()

        if (isItemSelected) {
            Glide.with(fragment)
                    .load(null)
                    .apply(RequestOptions.placeholderOf(R.drawable.ic_check_circle_grey_144dp))
                    .into(mediaImageView)
        } else {
            setTeamMedia()
        }

        animateCircularReveal(newScoutButton, !couldItemBeSelected)
        itemView.isActivated = !isItemSelected && !couldItemBeSelected && isScouting
        itemView.isSelected = isItemSelected
    }

    private fun setTeamNumber() {
        numberTextView.text = team.number
    }

    private fun setTeamName() = if (TextUtils.isEmpty(team.name)) {
        nameTextView.text = itemView.context.getString(R.string.unknown_team)
    } else {
        nameTextView.text = team.name
    }

    private fun setTeamMedia() = Glide.with(fragment)
            .load(team.media)
            .apply(RequestOptions.circleCropTransform()
                    .error(R.drawable.ic_memory_grey_48dp)
                    .fallback(R.drawable.ic_memory_grey_48dp))
            .listener(mediaLoadProgressListener)
            .into(mediaImageView)

    private fun setProgressVisibility(isMediaLoaded: Boolean = recyclerView.isScrolling || this.isMediaLoaded) {
        mediaLoadProgress.visibility = if (isMediaLoaded) View.GONE else View.VISIBLE
    }

    override fun onClick(v: View) {
        if (v.id == R.id.media || isItemSelected || couldItemBeSelected) {
            onTeamContextMenuRequested()
        } else {
            (itemView.context as TeamSelectionListener).onTeamSelected(
                    ScoutListFragmentBase.getBundle(team, v.id == R.id.new_scout, null), false)
        }
    }

    override fun onLongClick(v: View): Boolean {
        if (isItemSelected || couldItemBeSelected || v.id == R.id.root) {
            onTeamContextMenuRequested()
            return true
        } else if (v.id == R.id.media) {
            TeamDetailsDialog.show(fragment.childFragmentManager, team.helper)
            return true
        }

        return false
    }

    private fun onTeamContextMenuRequested() {
        isItemSelected = !isItemSelected
        updateItemStatus()
        menuManager.onTeamContextMenuRequested(team.helper)
    }

    override fun toString() = team.toString()
}

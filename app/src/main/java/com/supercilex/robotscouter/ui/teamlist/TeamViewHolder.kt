package com.supercilex.robotscouter.ui.teamlist

import android.content.Context
import android.graphics.drawable.Drawable
import android.support.annotation.Keep
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.ui.TeamDetailsDialog
import com.supercilex.robotscouter.util.data.getScoutBundle
import com.supercilex.robotscouter.util.ui.TeamSelectionListener
import com.supercilex.robotscouter.util.ui.animateCircularReveal
import com.supercilex.robotscouter.util.ui.views.ContentLoadingProgressBar
import kotterknife.bindView

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

    internal val mediaImageView: ImageView by bindView(R.id.media)
    private val mediaLoadProgress: ContentLoadingProgressBar by bindView(R.id.progress)
    private val numberTextView: TextView by bindView(R.id.number)
    private val nameTextView: TextView by bindView(R.id.name)
    private val newScoutButton: ImageButton by bindView(R.id.new_scout)

    private lateinit var team: Team
    private lateinit var fragment: Fragment
    private lateinit var recyclerView: RecyclerView
    private lateinit var menuHelper: TeamMenuHelper
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
             menuManager: TeamMenuHelper,
             isItemSelected: Boolean,
             couldItemBeSelected: Boolean,
             isScouting: Boolean) {
        this.team = team
        this.fragment = fragment
        this.recyclerView = recyclerView
        this.menuHelper = menuManager
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
        newScoutButton.setOnLongClickListener(this)
        itemView.setOnClickListener(this)
        itemView.setOnLongClickListener(this)
    }

    private fun updateItemStatus() {
        isMediaLoaded = isItemSelected
        setProgressVisibility()
        getTeamMediaRequestBuilder(isItemSelected, mediaImageView.context, team)
                .listener(mediaLoadProgressListener)
                .into(mediaImageView)

        animateCircularReveal(newScoutButton, !couldItemBeSelected)
        itemView.isActivated = !isItemSelected && !couldItemBeSelected && isScouting
        itemView.isSelected = isItemSelected
    }

    private fun setTeamNumber() {
        numberTextView.text = team.number.toString()
    }

    private fun setTeamName() = if (TextUtils.isEmpty(team.name)) {
        nameTextView.text = itemView.context.getString(R.string.team_unknown_team_title)
    } else {
        nameTextView.text = team.name
    }

    private fun setProgressVisibility(isMediaLoaded: Boolean = recyclerView.isScrolling || this.isMediaLoaded) =
            if (isMediaLoaded) mediaLoadProgress.hide(true) else mediaLoadProgress.show()

    override fun onClick(v: View) {
        if (v.id == R.id.media || isItemSelected || couldItemBeSelected) {
            onTeamContextMenuRequested()
        } else {
            (itemView.context as TeamSelectionListener)
                    .onTeamSelected(getScoutBundle(team, v.id == R.id.new_scout), false)
        }
    }

    override fun onLongClick(v: View): Boolean {
        when {
            isItemSelected || couldItemBeSelected || v.id == R.id.root -> onTeamContextMenuRequested()
            v.id == R.id.media -> TeamDetailsDialog.show(fragment.childFragmentManager, team)
            v.id == R.id.new_scout -> TeamTemplateSelectorDialog.show(
                    fragment.childFragmentManager, team)
            else -> return false
        }
        return true
    }

    private fun onTeamContextMenuRequested() {
        isItemSelected = !isItemSelected
        updateItemStatus()
        menuHelper.onTeamContextMenuRequested(team)
    }

    override fun toString() = team.toString()

    companion object {
        fun getTeamMediaRequestBuilder(isItemSelected: Boolean,
                                       context: Context,
                                       team: Team): RequestBuilder<Drawable> = if (isItemSelected) {
            Glide.with(context)
                    .load(null)
                    .apply(RequestOptions.placeholderOf(ContextCompat.getDrawable(
                            context, R.drawable.ic_check_circle_grey_56dp)))
        } else {
            Glide.with(context)
                    .load(team.media)
                    .apply(RequestOptions.circleCropTransform().error(ContextCompat.getDrawable(
                            context, R.drawable.ic_person_grey_96dp)))
        }

        private val RecyclerView.isScrolling get() = scrollState != RecyclerView.SCROLL_STATE_IDLE
    }
}

package com.supercilex.robotscouter.ui.teamlist

import android.content.Context
import android.graphics.drawable.Drawable
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.View
import android.view.ViewStub
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
import com.supercilex.robotscouter.util.ui.animatePopReveal
import com.supercilex.robotscouter.util.ui.views.ContentLoadingProgressBar
import com.supercilex.robotscouter.util.unsafeLazy
import kotterknife.bindView
import org.jetbrains.anko.find

class TeamViewHolder(
        itemView: View,
        private val fragment: Fragment,
        private val recyclerView: RecyclerView,
        private val menuHelper: TeamMenuHelper
) : RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener {
    private val unknownName: String by unsafeLazy {
        itemView.context.getString(R.string.team_unknown_team_title)
    }

    private val mediaLoadProgressListener = object : RequestListener<Drawable> {
        override fun onResourceReady(
                resource: Drawable?,
                model: Any?,
                target: Target<Drawable>,
                dataSource: DataSource,
                isFirstResource: Boolean
        ): Boolean {
            isMediaLoaded = true
            return false
        }

        override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable>,
                isFirstResource: Boolean
        ): Boolean {
            isMediaLoaded = true
            return false
        }
    }

    val mediaImageView: ImageView by bindView(R.id.media)
    private val mediaLoadProgressStub: ViewStub by bindView(R.id.progress)
    private val mediaLoadProgress by unsafeLazy {
        mediaLoadProgressStub.visibility = View.VISIBLE
        itemView.find<ContentLoadingProgressBar>(R.id.progress)
    }
    private val numberTextView: TextView by bindView(R.id.number)
    private val nameTextView: TextView by bindView(R.id.name)
    private val newScoutButton: ImageButton by bindView(R.id.new_scout)

    private lateinit var team: Team
    private var isItemSelected: Boolean = false
    private var couldItemBeSelected: Boolean = false
    private var isScouting: Boolean = false

    private var isMediaLoaded: Boolean = false
        set(value) {
            field = value
            updateProgressVisibility()
        }

    init {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) =
                    updateProgressVisibility()
        })

        mediaImageView.setOnClickListener(this)
        mediaImageView.setOnLongClickListener(this)
        newScoutButton.setOnClickListener(this)
        newScoutButton.setOnLongClickListener(this)
        itemView.setOnClickListener(this)
        itemView.setOnLongClickListener(this)
    }

    fun bind(
            team: Team,
            isItemSelected: Boolean,
            couldItemBeSelected: Boolean,
            isScouting: Boolean
    ) {
        this.team = team
        this.isItemSelected = isItemSelected
        this.couldItemBeSelected = couldItemBeSelected
        this.isScouting = isScouting

        setTeamNumber()
        setTeamName()
        updateItemStatus()
    }

    private fun setTeamNumber() {
        numberTextView.text = team.number.toString()
    }

    private fun setTeamName() {
        nameTextView.text = if (TextUtils.isEmpty(team.name)) unknownName else team.name
    }

    private fun updateItemStatus() {
        isMediaLoaded = isItemSelected
        updateProgressVisibility()
        getTeamMediaRequestBuilder(isItemSelected, mediaImageView.context, team)
                .listener(mediaLoadProgressListener)
                .into(mediaImageView)

        newScoutButton.animatePopReveal(!couldItemBeSelected)
        itemView.isActivated = !isItemSelected && !couldItemBeSelected && isScouting
        itemView.isSelected = isItemSelected
    }

    private fun updateProgressVisibility() {
        if (isMediaLoaded || recyclerView.isScrolling) {
            if (mediaLoadProgressStub.visibility != View.GONE) {
                mediaLoadProgress.hide(true)
            }
        } else {
            mediaLoadProgress.show()
        }
    }

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
        fun getTeamMediaRequestBuilder(
                isItemSelected: Boolean,
                context: Context,
                team: Team
        ): RequestBuilder<Drawable> = if (isItemSelected) {
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

package com.supercilex.robotscouter.feature.teams

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.supercilex.robotscouter.TeamSelectionListener
import com.supercilex.robotscouter.core.data.getScoutBundle
import com.supercilex.robotscouter.core.model.Team
import com.supercilex.robotscouter.core.ui.animatePopReveal
import com.supercilex.robotscouter.core.ui.setOnLongClickListenerCompat
import com.supercilex.robotscouter.core.unsafeLazy
import com.supercilex.robotscouter.shared.TeamDetailsDialog
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.team_list_row_layout.*
import java.util.Locale
import com.supercilex.robotscouter.R as RC

internal class TeamViewHolder(
        override val containerView: View,
        private val fragment: Fragment
) : RecyclerView.ViewHolder(containerView), LayoutContainer,
        View.OnClickListener, View.OnLongClickListener {
    private val unknownName: String by unsafeLazy {
        itemView.context.getString(R.string.team_unknown_team_title)
    }

    lateinit var team: Team
        private set
    private var isItemSelected: Boolean = false
    private var couldItemBeSelected: Boolean = false
    private var isScouting: Boolean = false

    init {
        media.setOnLongClickListenerCompat(this)
        newScout.setOnClickListener(this)
        newScout.setOnLongClickListenerCompat(this)
        itemView.setOnClickListener(this)
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
        number.text = String.format(Locale.getDefault(), "%d", team.number)
    }

    private fun setTeamName() {
        name.text = if (team.name?.isNotBlank() == true) team.name else unknownName
    }

    private fun updateItemStatus() {
        getTeamMediaRequestBuilder(isItemSelected, media.context, team).into(media)
        ViewCompat.setTransitionName(media, team.id)

        newScout.animatePopReveal(!couldItemBeSelected)
        itemView.isActivated = !isItemSelected && !couldItemBeSelected && isScouting
        itemView.isSelected = isItemSelected
    }

    override fun onClick(v: View) {
        if (!isItemSelected && !couldItemBeSelected) {
            (itemView.context as TeamSelectionListener)
                    .onTeamSelected(getScoutBundle(team, v.id == R.id.newScout), media)
        }
    }

    override fun onLongClick(v: View): Boolean {
        if (couldItemBeSelected) return false

        when {
            v.id == R.id.media -> TeamDetailsDialog.show(fragment.childFragmentManager, team)
            v.id == R.id.newScout -> TeamTemplateSelectorDialog.show(
                    fragment.childFragmentManager, team)
            else -> return false
        }
        return true
    }

    companion object {
        @SuppressLint("CheckResult") // Used in prefetch and standard load
        fun getTeamMediaRequestBuilder(
                isItemSelected: Boolean,
                context: Context,
                team: Team
        ): RequestBuilder<Drawable> = if (isItemSelected) {
            // TODO https://github.com/bumptech/glide/pull/3308
            // Glide.with(context).load(R.drawable.ic_check_circle_grey_56dp)
            Glide.with(context)
                    .load(null as String?)
                    .apply(RequestOptions.placeholderOf(R.drawable.ic_check_circle_grey_56dp))
        } else {
            Glide.with(context)
                    .load(team.media)
                    .apply(RequestOptions.circleCropTransform()
                                   .placeholder(RC.drawable.ic_person_grey_96dp)
                                   .error(RC.drawable.ic_person_grey_96dp))
                    .transition(DrawableTransitionOptions.withCrossFade())
        }
    }
}

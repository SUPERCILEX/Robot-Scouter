package com.supercilex.robotscouter.ui.teamlist;

import android.support.annotation.Keep;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.ui.TeamDetailsDialog;
import com.supercilex.robotscouter.ui.scout.ScoutListFragmentBase;
import com.supercilex.robotscouter.util.ViewUtils;

import de.hdodenhof.circleimageview.CircleImageView;

public class TeamViewHolder extends RecyclerView.ViewHolder
        implements View.OnClickListener, View.OnLongClickListener {
    private final CircleImageView mMedia;
    private final TextView mNumber;
    private final TextView mName;
    private final ImageButton mNewScout;

    private Team mTeam;
    private Fragment mFragment;
    private TeamMenuManager mMenuManager;
    private boolean mIsItemSelected;
    private boolean mCouldItemBeSelected;
    private boolean mIsScouting;

    @Keep
    public TeamViewHolder(View itemView) {
        super(itemView);
        mMedia = (CircleImageView) itemView.findViewById(R.id.media);
        mNumber = (TextView) itemView.findViewById(R.id.number);
        mName = (TextView) itemView.findViewById(R.id.name);
        mNewScout = (ImageButton) itemView.findViewById(R.id.new_scout);
    }

    public boolean isScouting() {
        return mIsScouting;
    }

    public void bind(Team team,
                     Fragment fragment,
                     TeamMenuManager menuManager,
                     boolean isItemSelected,
                     boolean couldItemBeSelected,
                     boolean isScouting) {
        mTeam = team;
        mFragment = fragment;
        mMenuManager = menuManager;
        mIsItemSelected = isItemSelected;
        mCouldItemBeSelected = couldItemBeSelected;
        mIsScouting = isScouting;

        setTeamNumber();
        setTeamName();
        updateItemStatus();

        mMedia.setOnClickListener(this);
        mMedia.setOnLongClickListener(this);
        mNewScout.setOnClickListener(this);
        itemView.setOnClickListener(this);
        itemView.setOnLongClickListener(this);
    }

    private void updateItemStatus() {
        if (mIsItemSelected) {
            Glide.with(itemView.getContext())
                    .load("")
                    .placeholder(ContextCompat.getDrawable(itemView.getContext(),
                                                           R.drawable.ic_check_circle_grey_144dp))
                    .into(mMedia);
        } else {
            setTeamMedia();
        }

        ViewUtils.animateCircularReveal(mNewScout, !mCouldItemBeSelected);
        itemView.setActivated(!mIsItemSelected && !mCouldItemBeSelected && mIsScouting);
        itemView.setSelected(mIsItemSelected);
    }

    private void setTeamNumber() {
        mNumber.setText(mTeam.getNumber());
    }

    private void setTeamName() {
        if (TextUtils.isEmpty(mTeam.getName())) {
            mName.setText(itemView.getContext().getString(R.string.unknown_team));
        } else {
            mName.setText(mTeam.getName());
        }
    }

    private void setTeamMedia() {
        Glide.with(itemView.getContext())
                .load(mTeam.getMedia())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .error(R.drawable.ic_memory_grey_48dp)
                .into(mMedia);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.media || mIsItemSelected || mCouldItemBeSelected) {
            onTeamContextMenuRequested();
        } else {
            ((TeamSelectionListener) itemView.getContext()).onTeamSelected(
                    ScoutListFragmentBase.getBundle(mTeam, v.getId() == R.id.new_scout, null), false);
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (mIsItemSelected || mCouldItemBeSelected || v.getId() == R.id.root) {
            onTeamContextMenuRequested();
            return true;
        } else if (v.getId() == R.id.media) {
            TeamDetailsDialog.show(mFragment.getChildFragmentManager(), mTeam.getHelper());
            return true;
        }

        return false;
    }

    private void onTeamContextMenuRequested() {
        mIsItemSelected = !mIsItemSelected;
        updateItemStatus();
        mMenuManager.onTeamContextMenuRequested(mTeam.getHelper());
    }

    @Override
    public String toString() {
        return mTeam.toString();
    }
}

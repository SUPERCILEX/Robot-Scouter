package com.supercilex.robotscouter.ui.teamlist;

import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
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
import com.supercilex.robotscouter.ui.scout.ScoutActivity;

import de.hdodenhof.circleimageview.CircleImageView;

public class TeamViewHolder extends RecyclerView.ViewHolder
        implements View.OnClickListener, View.OnLongClickListener {
    private Team mTeam;
    private Fragment mFragment;
    private TeamMenuManager mMenuManager;
    private boolean mIsItemSelected;
    private boolean mCouldItemBeSelected;

    private CircleImageView mLogo;
    private TextView mNumber;
    private TextView mName;
    private ImageButton mNewScout;

    @Keep
    public TeamViewHolder(View itemView) {
        super(itemView);
        mLogo = (CircleImageView) itemView.findViewById(R.id.logo);
        mNumber = (TextView) itemView.findViewById(R.id.number);
        mName = (TextView) itemView.findViewById(R.id.name);
        mNewScout = (ImageButton) itemView.findViewById(R.id.new_scout);
    }

    public void bind(Team team,
                     Fragment fragment,
                     TeamMenuManager menuManager,
                     boolean isItemSelected,
                     boolean couldItemBeSelected) {
        mTeam = team;
        mFragment = fragment;
        mMenuManager = menuManager;
        mIsItemSelected = isItemSelected;
        mCouldItemBeSelected = couldItemBeSelected;

        mNewScout.setBackground(getRippleDrawable());
        setTeamNumber();
        setTeamName();
        updateItemStatus();

        mLogo.setOnClickListener(this);
        mNewScout.setOnClickListener(this);
        itemView.setOnClickListener(this);
        itemView.setOnLongClickListener(this);
    }

    private void updateItemStatus() {
        itemView.setBackground(getRippleDrawable());
        if (mIsItemSelected) {
            Glide.with(mFragment)
                    .load("")
                    .placeholder(ContextCompat.getDrawable(mFragment.getContext(),
                                                           R.drawable.ic_check_circle_grey_144dp))
                    .into(mLogo);
            itemView.setBackgroundColor(Color.parseColor("#462a56c6")); // Tinted blue
        } else {
            setTeamLogo();
        }
        mNewScout.setVisibility(mCouldItemBeSelected ? View.GONE : View.VISIBLE);
    }

    private Drawable getRippleDrawable() {
        int[] attrs = new int[]{android.R.attr.selectableItemBackground};
        TypedArray typedArray = mFragment.getContext().obtainStyledAttributes(attrs);
        try {
            return typedArray.getDrawable(0);
        } finally {
            typedArray.recycle();
        }
    }

    private void setTeamNumber() {
        mNumber.setText(mTeam.getNumber());
    }

    private void setTeamName() {
        if (TextUtils.isEmpty(mTeam.getName())) {
            mName.setText(mFragment.getString(R.string.unknown_team));
        } else {
            mName.setText(mTeam.getName());
        }
    }

    private void setTeamLogo() {
        Glide.with(mFragment)
                .load(mTeam.getMedia())
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .error(R.drawable.ic_android_black_144dp)
                .into(mLogo);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.logo || mIsItemSelected || mCouldItemBeSelected) {
            onTeamContextMenuRequested();
        } else {
            ScoutActivity.start(mFragment.getContext(),
                                mTeam.getHelper(),
                                v.getId() == R.id.new_scout);
        }
    }

    @Override
    public boolean onLongClick(View v) {
        onTeamContextMenuRequested();
        return true;
    }

    private void onTeamContextMenuRequested() {
        mIsItemSelected = !mIsItemSelected;
        updateItemStatus();
        mMenuManager.onTeamContextMenuRequested(mTeam.getHelper());
    }

    @Override
    public String toString() {
        return "TeamViewHolder{" +
                "mTeam=" + mTeam +
                ", mFragment=" + mFragment +
                ", mMenuManager=" + mMenuManager +
                ", mIsItemSelected=" + mIsItemSelected +
                ", mCouldItemBeSelected=" + mCouldItemBeSelected +
                ", mLogo=" + mLogo +
                ", mNumber=" + mNumber +
                ", mName=" + mName +
                ", mNewScout=" + mNewScout +
                '}';
    }
}

package com.supercilex.robotscouter.ui.teamlist;

import android.content.Context;
import android.support.annotation.Keep;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.ui.scout.ScoutActivity;

public class TeamHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    private Context mContext;
    private Team mTeam;

    private ConstraintLayout mRowLayout;
    private TextView mNumber;
    private TextView mName;
    private ImageView mLogo;
    private ImageButton mNewScout;

    @Keep
    public TeamHolder(View itemView) {
        super(itemView);
        mRowLayout = (ConstraintLayout) itemView;
        mNumber = (TextView) itemView.findViewById(R.id.number);
        mName = (TextView) itemView.findViewById(R.id.name);
        mLogo = (ImageView) itemView.findViewById(R.id.logo);
        mNewScout = (ImageButton) itemView.findViewById(R.id.new_scout);
    }

    public void bind(Context context, Team team) {
        mContext = context;
        mTeam = team;
        setTeamNumber();
        setTeamName();
        setTeamLogo();
        mRowLayout.setOnClickListener(this);
        mNewScout.setOnClickListener(this);
    }

    private void setTeamNumber() {
        mNumber.setText(mTeam.getNumber());
    }

    private void setTeamName() {
        if (mTeam.getName() != null) {
            mName.setText(mTeam.getName());
        } else {
            mName.setText(mContext.getString(R.string.unknown_team));
        }
    }

    private void setTeamLogo() {
        Glide.with(mContext)
                .load(mTeam.getMedia())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .error(R.drawable.ic_android_black_24dp)
                .into(mLogo);
    }

    @Override
    public void onClick(View v) {
        ScoutActivity.start(mContext, mTeam, v.getId() == R.id.new_scout);
    }
}

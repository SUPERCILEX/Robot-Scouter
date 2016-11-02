package com.supercilex.robotscouter.ui.teamlist;

import android.app.Activity;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.Scout;
import com.supercilex.robotscouter.ui.scout.ScoutActivity;

public class TeamHolder extends RecyclerView.ViewHolder {
    private ConstraintLayout mRowLayout;
    private TextView mNumber;
    private TextView mName;
    private ImageView mLogo;
    private ImageButton mNewScout;

    public TeamHolder(View itemView) {
        super(itemView);
        mRowLayout = (ConstraintLayout) itemView;
        mNumber = (TextView) itemView.findViewById(R.id.list_view_layout_team_number);
        mName = (TextView) itemView.findViewById(R.id.list_view_layout_team_nickname);
        mLogo = (ImageView) itemView.findViewById(R.id.logo);
        mNewScout = (ImageButton) itemView.findViewById(R.id.list_view_layout_start_scout);
    }

    public void setTeamNumber(String teamNumber) {
        mNumber.setText(teamNumber);
    }

    public void setTeamName(String teamName, String noName) {
        if (teamName != null) {
            mName.setText(teamName);
        } else {
            mName.setText(noName);
        }
    }

    public void setTeamLogo(Activity activity, String teamLogo) {
        Glide.with(activity)
                .load(teamLogo)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .error(R.drawable.ic_android_black_24dp)
                .into(mLogo);
    }

    public void setListItemClickListener(final Activity activity,
                                         final String teamNumber,
                                         final String key) {
        mRowLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.startActivity(ScoutActivity.createIntent(activity, teamNumber, key));
            }
        });
    }

    public void setCreateNewScoutListener(final Activity activity,
                                          final String teamNumber,
                                          final String key) {
        mNewScout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Scout().createScoutId(teamNumber);
                activity.startActivity(ScoutActivity.createIntent(activity, teamNumber, key));
            }
        });
    }
}

package com.supercilex.robotscouter.model.team;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.supercilex.robotscouter.Constants;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.model.scout.Scout;
import com.supercilex.robotscouter.scout.ScoutActivity;

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
        mLogo = (ImageView) itemView.findViewById(R.id.list_view_layout_team_logo);
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

    public void setTeamLogo(String teamLogo, Activity activity) {
        Glide.with(activity)
                .load(teamLogo)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .error(R.drawable.ic_android_black_24dp)
                .into(mLogo);
    }

    public void setListItemClickListener(final String teamNumber,
                                         final Activity activity,
                                         final String key) {
        mRowLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startScout(activity, teamNumber, key);
            }
        });
    }

    public void setCreateNewScoutListener(final String teamNumber,
                                          final Activity activity,
                                          final String key) {
        mNewScout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Scout().createScoutId(teamNumber);
                startScout(activity, teamNumber, key);
            }
        });
    }

    private void startScout(Activity activity, String teamNumber, String key) {
        Intent intent = new Intent(activity, ScoutActivity.class);
        intent.putExtra(Constants.INTENT_TEAM_NUMBER, teamNumber);
        intent.putExtra(Constants.INTENT_TEAM_KEY, key);

        intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);
        }

        activity.startActivity(intent);
    }
}

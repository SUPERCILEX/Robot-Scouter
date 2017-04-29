package com.supercilex.robotscouter.ui.scout;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.util.TeamHelper;

public class ScoutActivity extends AppCompatActivity {
    public static void start(Context context, TeamHelper teamHelper, boolean addScout) {
        Intent starter = teamHelper.toIntent().setClass(context, ScoutActivity.class);
        starter.putExtra(ScoutListFragmentBase.ADD_SCOUT_KEY, addScout);

        starter.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            starter.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            starter.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);
        }

        context.startActivity(starter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.RobotScouter_NoActionBar_TransparentStatusBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scout);
        if (savedInstanceState == null) {
            Intent intent = getIntent();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.scouts, ActivityScoutListFragment.newInstance(
                            TeamHelper.parse(intent),
                            intent.getBooleanExtra(ScoutListFragmentBase.ADD_SCOUT_KEY, false)))
                    .commit();
        }
    }
}

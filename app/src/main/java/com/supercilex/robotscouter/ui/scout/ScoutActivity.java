package com.supercilex.robotscouter.ui.scout;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.Team;

import static com.supercilex.robotscouter.ui.scout.ScoutListFragmentBase.KEY_SCOUT_ARGS;

public class ScoutActivity extends AppCompatActivity {
    public static void start(Context context, Team team, boolean addScout) {
        context.startActivity(createIntent(
                context, ScoutListFragmentBase.getBundle(team, addScout, null)));
    }

    public static Intent createIntent(Context context, Bundle args) {
        Intent starter = new Intent(context, ScoutActivity.class).putExtra(KEY_SCOUT_ARGS, args);

        starter.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            starter.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            starter.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);
        }

        return starter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.RobotScouter_NoActionBar_TransparentStatusBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scout);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.scouts, ActivityScoutListFragment.newInstance(
                            getIntent().getBundleExtra(KEY_SCOUT_ARGS)))
                    .commit();
        }
    }
}

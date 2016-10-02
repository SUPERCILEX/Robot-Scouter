package com.supercilex.robotscouter;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.supercilex.robotscouter.ui.teamlist.TeamListActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;

@RunWith(AndroidJUnit4.class)
public class TmpTest {
    @Rule
    public ActivityTestRule<TeamListActivity> mActivityTestRule = new ActivityTestRule<>(
            TeamListActivity.class);

    @Test
    public void tmpTest() {
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
    }
}

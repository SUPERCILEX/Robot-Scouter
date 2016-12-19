package com.supercilex.robotscouter.ui.scout.template;

import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;

import com.google.firebase.database.Query;
import com.supercilex.robotscouter.ui.scout.ScoutPagerAdapter;

public class ScoutTemplatesPagerAdapter extends ScoutPagerAdapter {
    public ScoutTemplatesPagerAdapter(FragmentManager fm, TabLayout tabLayout, Query query) {
        super(fm, tabLayout, query);
    }
}

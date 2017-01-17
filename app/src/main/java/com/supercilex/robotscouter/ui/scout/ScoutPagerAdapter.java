package com.supercilex.robotscouter.ui.scout;

import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.supercilex.robotscouter.data.util.ScoutUtils;

import java.util.ArrayList;
import java.util.List;

public class ScoutPagerAdapter extends FragmentStatePagerAdapter
        implements ValueEventListener, TabLayout.OnTabSelectedListener {
    private List<String> mKeys = new ArrayList<>();
    private String mCurrentScoutKey;
    private TabLayout mTabLayout;
    private Query mQuery;

    public ScoutPagerAdapter(FragmentManager fm,
                             TabLayout tabLayout,
                             String teamKey,
                             String currentScoutKey) {
        super(fm);
        mTabLayout = tabLayout;
        mCurrentScoutKey = currentScoutKey;
        mQuery = ScoutUtils.getIndicesRef(teamKey);
        mQuery.addValueEventListener(this);
    }

    @Override
    public Fragment getItem(int position) {
        return ScoutFragment.newInstance(mQuery.getRef().getKey(), mKeys.get(position));
    }

    @Override
    public int getCount() {
        return mKeys.size();
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return "SCOUT " + (getCount() - position);
    }

    public String getCurrentScoutKey() {
        return mCurrentScoutKey;
    }

    public void setCurrentScoutKey(String currentScoutKey) {
        mCurrentScoutKey = currentScoutKey;
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        mCurrentScoutKey = mKeys.get(tab.getPosition());
    }

    @Override
    public void onDataChange(DataSnapshot snapshot) {
        mKeys.clear();
        for (DataSnapshot scoutIndex : snapshot.getChildren()) {
            mKeys.add(0, scoutIndex.getKey());
        }

        mTabLayout.removeOnTabSelectedListener(this);
        notifyDataSetChanged();
        mTabLayout.addOnTabSelectedListener(this);

        if (mKeys.isEmpty()) return;
        if (mCurrentScoutKey == null) {
            selectTab(0);
            mCurrentScoutKey = mKeys.get(0);
        } else {
            selectTab(mKeys.indexOf(mCurrentScoutKey));
        }
    }

    private void selectTab(int index) {
        TabLayout.Tab tab = mTabLayout.getTabAt(index);
        if (tab != null) tab.select();
    }

    public void cleanup() {
        mQuery.removeEventListener(this);
    }

    @Override
    public void onCancelled(DatabaseError error) {
        FirebaseCrash.report(error.toException());
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
        // Not interesting
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {
        // Not interesting
    }
}

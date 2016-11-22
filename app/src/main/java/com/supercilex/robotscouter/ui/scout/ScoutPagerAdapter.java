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

import java.util.ArrayList;
import java.util.List;

public class ScoutPagerAdapter extends FragmentStatePagerAdapter implements ValueEventListener {
    private static final int SAVE_STATE = 1;
    private static final int UPDATE = 0;

    private List<String> mKeys = new ArrayList<>();
    private TabLayout mTabLayout;
    private String mSavedTabKey;
    private boolean mManuallyAddedTab;
    private Query mQuery;

    public ScoutPagerAdapter(FragmentManager fm, TabLayout tabLayout, Query query, String number) {
        super(fm);
        mTabLayout = tabLayout;
        mQuery = query;
        mQuery.orderByValue().equalTo(Long.parseLong(number)).addValueEventListener(this);
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    @Override
    public Fragment getItem(int position) {
        return ScoutFragment.newInstance(mKeys.get(position));
    }

    @Override
    public int getCount() {
        return mKeys.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return "SCOUT " + (getCount() - position);
    }

    @Override
    public void onDataChange(DataSnapshot snapshot) {
        if (snapshot.getValue() != null) {
            String selectedTabKey = getSelectedTabKey();
            mKeys.clear();
            for (DataSnapshot scoutIndex : snapshot.getChildren()) {
                mKeys.add(0, scoutIndex.getKey());
            }

            notifyDataSetChanged();
            if (!mManuallyAddedTab) {
                if (mSavedTabKey != null) {
                    selectTab(mSavedTabKey, SAVE_STATE);
                    mSavedTabKey = null; // NOPMD todo maybe?
                } else if (selectedTabKey != null) {
                    selectTab(selectedTabKey, UPDATE);
                }
            } else {
                TabLayout.Tab tab = mTabLayout.getTabAt(0);
                if (tab != null) tab.select();
                mManuallyAddedTab = false;
            }
        }
    }

    public void cleanup() {
        mQuery.removeEventListener(this);
    }

    public String getSelectedTabKey() {
        if (mTabLayout.getSelectedTabPosition() != -1) {
            return mKeys.get((getCount() - 1) - mTabLayout.getSelectedTabPosition());
        } else {
            return null;
        }
    }

    public void setSavedTabKey(String savedTabKey) {
        mSavedTabKey = savedTabKey;
    }

    public void setManuallyAddedScout() {
        mManuallyAddedTab = true;
    }

    private void selectTab(String selectedTabKey, int adjust) {
        TabLayout.Tab tab = mTabLayout.getTabAt(getCount() -
                                                        (mKeys.indexOf(selectedTabKey) + adjust));
        if (tab != null) tab.select();
    }

    @Override
    public void onCancelled(DatabaseError error) {
        FirebaseCrash.report(error.toException());
    }
}

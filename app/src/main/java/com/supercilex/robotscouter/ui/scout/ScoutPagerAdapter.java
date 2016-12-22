package com.supercilex.robotscouter.ui.scout;

import android.content.Context;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.supercilex.robotscouter.data.model.Scout;
import com.supercilex.robotscouter.util.BaseHelper;

import java.util.ArrayList;
import java.util.List;

public class ScoutPagerAdapter extends FragmentStatePagerAdapter implements ValueEventListener {
    private static final int SAVE_STATE = 1;
    private static final int UPDATE = 0;

    private List<String> mKeys = new ArrayList<>();

    private TabLayout mTabLayout;
    private String mSavedTabKey;
    private boolean mIsManuallyAddedTab;

    private Query mQuery;
    private String mTeamNumber;

    public ScoutPagerAdapter(FragmentManager fm,
                             TabLayout tabLayout,
                             String teamNumber,
                             Context context) {
        super(fm);
        mTabLayout = tabLayout;
        mTeamNumber = teamNumber;
        if (BaseHelper.isOffline(context)) {
            mQuery = Scout.getIndicesRef();
        } else {
            mQuery = Scout.getIndicesRef().orderByValue().equalTo(Long.parseLong(mTeamNumber));
        }
        mQuery.addValueEventListener(this);
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
        String selectedTabKey = getSelectedTabKey();
        mKeys.clear();
        for (DataSnapshot scoutIndex : snapshot.getChildren()) {
            if (scoutIndex.getValue().toString().equals(mTeamNumber)) {
                mKeys.add(0, scoutIndex.getKey());
            }
        }

        notifyDataSetChanged();
        if (mIsManuallyAddedTab) {
            TabLayout.Tab tab = mTabLayout.getTabAt(0);
            if (tab != null) tab.select();
            mIsManuallyAddedTab = false;
        } else {
            if (mSavedTabKey != null) {
                selectTab(mSavedTabKey, SAVE_STATE);
                mSavedTabKey = null; // NOPMD todo maybe?
            } else if (selectedTabKey != null) {
                selectTab(selectedTabKey, UPDATE);
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
        mIsManuallyAddedTab = true;
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

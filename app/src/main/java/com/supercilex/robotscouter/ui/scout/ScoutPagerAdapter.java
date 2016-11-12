package com.supercilex.robotscouter.ui.scout;

import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.ArrayList;
import java.util.List;

class ScoutPagerAdapter extends FragmentStatePagerAdapter {
    private List<String> mKeyList = new ArrayList<>();
    private TabLayout mTabLayout;

    ScoutPagerAdapter(FragmentManager fm, TabLayout tabLayout) {
        super(fm);
        mTabLayout = tabLayout;
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    @Override
    public Fragment getItem(int position) {
        // getItem is called to instantiate the fragment for the given page.
        // Return a PlaceholderFragment (defined as a static inner class below).
        return ScoutFragment.newInstance(mKeyList.get(position));
    }

    @Override
    public int getCount() {
        return mKeyList.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return "SCOUT " + (getCount() - position);
    }

    void add(String key) {
        TabLayout.Tab tab = mTabLayout.getTabAt(0);
        mKeyList.add(0, key);
        notifyDataSetChanged();
        if (tab != null) tab.select();
    }

    void remove(String key) {
        TabLayout.Tab tab = mTabLayout.getTabAt(mKeyList.indexOf(key));
        mKeyList.remove(key);
        notifyDataSetChanged();
        if (tab != null) tab.select();
    }
}

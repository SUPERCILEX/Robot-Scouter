package com.supercilex.robotscouter.ui.scout;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.ArrayList;
import java.util.List;

class ScoutPagerAdapter extends FragmentStatePagerAdapter {
    private List<String> mKeyList = new ArrayList<>();

    ScoutPagerAdapter(FragmentManager fm) {
        super(fm);
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
        mKeyList.add(0, key);
        notifyDataSetChanged();
    }

    void remove(String key) {
        mKeyList.remove(key);
        notifyDataSetChanged();
    }
}

package com.supercilex.robotscouter.ui;

import android.support.v4.app.Fragment;

import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.util.BaseHelper;

public class FragmentHelper extends BaseHelper {
    private Fragment mFragment;

    public FragmentHelper(Fragment fragment) {
        super();
        mFragment = fragment;
    }

    public Team getTeam() {
        return BaseHelper.getTeam(mFragment.getArguments());
    }
}

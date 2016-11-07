package com.supercilex.robotscouter.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

public class FragmentBase extends Fragment {
    private FragmentHelper mHelper;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHelper = new FragmentHelper(this);
    }

    public FragmentHelper getHelper() {
        return mHelper;
    }
}

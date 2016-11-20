package com.supercilex.robotscouter.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;

public class StickyFragment extends FragmentBase {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }
}

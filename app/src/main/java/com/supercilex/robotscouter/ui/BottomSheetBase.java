package com.supercilex.robotscouter.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;

public class BottomSheetBase extends BottomSheetDialogFragment {
    protected FragmentHelper mHelper;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHelper = new FragmentHelper(this);
    }
}

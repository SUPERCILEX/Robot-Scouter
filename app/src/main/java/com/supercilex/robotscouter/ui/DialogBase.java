package com.supercilex.robotscouter.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;

public class DialogBase extends DialogFragment {
    protected DialogHelper mHelper;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHelper = new DialogHelper(this);
    }
}

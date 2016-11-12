package com.supercilex.robotscouter.ui;

import android.support.v4.app.DialogFragment;
import android.view.WindowManager;

public class DialogHelper extends FragmentHelper {
    private DialogFragment mDialog;

    public DialogHelper(DialogFragment fragment) {
        super(fragment);
        mDialog = fragment;
    }

    public void showKeyboard() {
        mDialog.getDialog().getWindow()
                .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }
}

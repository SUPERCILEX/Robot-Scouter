package com.supercilex.robotscouter.ui;

import android.os.Bundle;

public class VisibleKeyboardDialog extends DialogBase {
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mHelper.showKeyboard();
    }
}

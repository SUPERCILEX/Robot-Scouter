package com.supercilex.robotscouter.ui.scout;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.TextInputLayout;
import android.view.View;
import android.widget.EditText;

import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.RobotScouter;
import com.supercilex.robotscouter.ui.KeyboardDialogBase;
import com.supercilex.robotscouter.util.DatabaseHelper;

public abstract class ScoutValueDialogBase<T> extends KeyboardDialogBase implements Runnable {
    protected static final String CURRENT_VALUE = "current_value";

    protected TextInputLayout mInputLayout;
    protected EditText mValue;

    protected abstract T getValue();

    @StringRes
    protected abstract int getTitle();

    @StringRes
    protected abstract int getHint();

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View rootView = View.inflate(getContext(), R.layout.dialog_scout_value, null);
        mInputLayout =
                (TextInputLayout) rootView.findViewById(R.id.value_layout);
        mInputLayout.setHint(getString(getHint()));
        mValue = (EditText) mInputLayout.findViewById(R.id.value);
        mValue.setText(getArguments().getString(CURRENT_VALUE));
        mValue.post(this);

        return createDialog(rootView, getTitle());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        RobotScouter.getRefWatcher(getActivity()).watch(this);
    }

    @Override
    public void run() {
        mValue.selectAll();
    }

    @Override
    protected EditText getLastEditText() {
        return mValue;
    }

    @Override
    protected boolean onClick() {
        DatabaseHelper.getRef(getArguments()).setValue(getValue());
        return true;
    }
}

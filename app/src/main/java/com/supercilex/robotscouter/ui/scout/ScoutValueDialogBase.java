package com.supercilex.robotscouter.ui.scout;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.TextInputLayout;
import android.view.View;
import android.widget.EditText;

import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.ui.common.KeyboardDialogBase;
import com.supercilex.robotscouter.util.DatabaseHelper;

public abstract class ScoutValueDialogBase<T> extends KeyboardDialogBase {
    protected static final String CURRENT_VALUE = "current_value";

    protected EditText mValue;

    protected abstract T getValue();

    @StringRes
    protected abstract int getTitle();

    @StringRes
    protected abstract int getHint();

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        TextInputLayout layout =
                (TextInputLayout) View.inflate(getContext(), R.layout.dialog_scout_value, null);
        layout.setHint(getString(getHint()));
        mValue = (EditText) layout.findViewById(R.id.value);
        mValue.setText(getArguments().getString(CURRENT_VALUE));

        return createDialog(layout, getTitle());
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

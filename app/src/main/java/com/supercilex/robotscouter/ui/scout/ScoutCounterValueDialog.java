package com.supercilex.robotscouter.ui.scout;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.text.InputType;

import com.google.firebase.database.DatabaseReference;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.util.DatabaseHelper;

import java.math.BigDecimal;

public class ScoutCounterValueDialog extends ScoutValueDialogBase<Integer> {
    private static final String TAG = "ScoutCounterValueDialog";

    public static void show(FragmentManager manager, DatabaseReference ref, String currentValue) {
        ScoutValueDialogBase dialog = new ScoutCounterValueDialog();

        Bundle args = DatabaseHelper.getRefBundle(ref);
        args.putString(CURRENT_VALUE, currentValue);
        dialog.setArguments(args);

        dialog.show(manager, TAG);
    }

    @Override
    public void onShow(DialogInterface dialog) {
        super.onShow(dialog);
        mValue.setInputType(InputType.TYPE_CLASS_NUMBER);
    }

    @Override
    protected boolean onClick() {
        try {
            new BigDecimal(mValue.getText().toString()).intValueExact();
            return super.onClick();
        } catch (NumberFormatException | ArithmeticException e) {
            mInputLayout.setError(getString(R.string.invalid_team_number));
            return false;
        }
    }

    @Override
    protected Integer getValue() {
        return Integer.valueOf(mValue.getText().toString());
    }

    @Override
    protected int getTitle() {
        return R.string.edit_value;
    }

    @Override
    protected int getHint() {
        return R.string.value;
    }
}

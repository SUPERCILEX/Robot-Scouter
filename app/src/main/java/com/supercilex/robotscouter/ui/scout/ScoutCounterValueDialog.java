package com.supercilex.robotscouter.ui.scout;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.text.InputType;

import com.google.firebase.database.DatabaseReference;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.util.DatabaseHelper;

public class ScoutCounterValueDialog extends ScoutValueDialogBase<Integer> {
    private static final String TAG = "ScoutCounterValueDialog";

    public static void show(FragmentManager manager, DatabaseReference ref, String currentValue) {
        ScoutValueDialogBase dialog = new ScoutCounterValueDialog();

        Bundle bundle = DatabaseHelper.getRefBundle(ref);
        bundle.putString(CURRENT_VALUE, currentValue);
        dialog.setArguments(bundle);

        dialog.show(manager, TAG);
    }

    @Override
    public void onShow(DialogInterface dialog) {
        super.onShow(dialog);
        mValue.setInputType(InputType.TYPE_CLASS_NUMBER);
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

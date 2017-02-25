package com.supercilex.robotscouter.ui.scout;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.text.InputType;
import android.text.TextUtils;

import com.google.firebase.database.DatabaseReference;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.util.DatabaseHelper;

public class ScoutNameDialog extends ScoutValueDialogBase<String> {
    private static final String TAG = "ScoutNameDialog";

    public static void show(FragmentManager manager, DatabaseReference ref, String currentValue) {
        ScoutValueDialogBase dialog = new ScoutNameDialog();

        Bundle args = DatabaseHelper.getRefBundle(ref);
        args.putString(CURRENT_VALUE, currentValue);
        dialog.setArguments(args);

        dialog.show(manager, TAG);
    }

    @Override
    public void onShow(DialogInterface dialog) {
        super.onShow(dialog);
        mValue.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
    }

    @Override
    protected String getValue() {
        String name = mValue.getText().toString();
        return TextUtils.isEmpty(name) ? null : name;
    }

    @Override
    protected int getTitle() {
        return R.string.edit_scout_name;
    }

    @Override
    protected int getHint() {
        return R.string.scout_name;
    }
}

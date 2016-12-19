package com.supercilex.robotscouter.ui.teamlist;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.ui.KeyboardDialog;
import com.supercilex.robotscouter.ui.scout.ScoutActivity;

public class NewTeamDialog extends KeyboardDialog implements TextView.OnEditorActionListener {
    private static final String TAG = "NewTeamDialog";

    private TextInputLayout mInputLayout;

    public static void show(FragmentManager manager) {
        new NewTeamDialog().show(manager, TAG);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View rootView = View.inflate(getContext(), R.layout.dialog_new_team, null);
        mInputLayout = (TextInputLayout) rootView.findViewById(R.id.input_layout);
        mInputLayout.getEditText().setOnEditorActionListener(this);
        return createDialog(rootView, R.string.new_scout);
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (event != null
                && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                || (actionId == EditorInfo.IME_ACTION_DONE)
                && !TextUtils.isEmpty(mInputLayout.getEditText().getText())) {
            onClick();
            return true;
        }
        return false;
    }

    @Override
    public boolean onClick() {
        String teamNumber = mInputLayout.getEditText().getText().toString();
        if (isValid(teamNumber)) {
            teamNumber = String.valueOf(Long.parseLong(teamNumber));
            ScoutActivity.start(getContext(), new Team.Builder(teamNumber).build(), true);
            return true;
        } else {
            mInputLayout.setError(getString(R.string.invalid_team_number));
            return false;
        }
    }

    private boolean isValid(String teamNumber) {
        try {
            long number = Long.parseLong(teamNumber);
            return number >= 0 && number <= 100000;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}

package com.supercilex.robotscouter.ui.teamlist;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.ui.KeyboardDialogBase;
import com.supercilex.robotscouter.ui.scout.ScoutActivity;
import com.supercilex.robotscouter.util.AnalyticsHelper;

public class NewTeamDialog extends KeyboardDialogBase {
    private static final String TAG = "NewTeamDialog";

    private TextInputLayout mInputLayout;
    private EditText mTeamNumberEditText;

    public static void show(FragmentManager manager) {
        new NewTeamDialog().show(manager, TAG);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mInputLayout = (TextInputLayout) View.inflate(getContext(), R.layout.dialog_new_team, null);
        mTeamNumberEditText = (EditText) mInputLayout.findViewById(R.id.team_number);
        return createDialog(mInputLayout, R.string.add_scout);
    }

    @Override
    protected EditText getLastEditText() {
        return mTeamNumberEditText;
    }

    @Override
    public boolean onClick() {
        String teamNumber = mTeamNumberEditText.getText().toString();
        if (isValid(teamNumber)) {
            teamNumber = String.valueOf(Long.parseLong(teamNumber));
            ScoutActivity.start(getContext(),
                                new Team.Builder(teamNumber).build().getHelper(),
                                true);
            AnalyticsHelper.selectTeam(teamNumber);
            return true;
        } else {
            mInputLayout.setError(getString(R.string.invalid_team_number));
            return false;
        }
    }

    private boolean isValid(String teamNumber) {
        if (TextUtils.isEmpty(teamNumber)) return false;

        try {
            long number = Long.parseLong(teamNumber);
            return number >= 0 && number <= 100000;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}

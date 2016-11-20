package com.supercilex.robotscouter.ui.teamlist;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.view.View;

import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.Scout;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.ui.KeyboardDialog;
import com.supercilex.robotscouter.ui.scout.ScoutActivity;

public class NewTeamDialog extends KeyboardDialog {
    private TextInputLayout mInputLayout;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View rootView = View.inflate(getContext(), R.layout.dialog_new_team, null);
        mInputLayout = (TextInputLayout) rootView.findViewById(R.id.input_layout);
        setOnEditorActionListener(mInputLayout.getEditText());
        return createDialog(rootView, R.string.new_scout);
    }

    @Override
    public boolean onClick() {
        String teamNumber = mInputLayout.getEditText().getText().toString();
        if (isValid(teamNumber)) {
            teamNumber = String.valueOf(Long.parseLong(teamNumber));
            new Scout().createScoutId(teamNumber);
            startActivity(ScoutActivity.createIntent(getContext(), new Team(teamNumber)));
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

package com.supercilex.robotscouter.ui.teamlist;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.Scout;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.ui.scout.ScoutActivity;

public class NewTeamDialogFragment extends DialogFragment {
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getDialog().getWindow()
                .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View rootView = View.inflate(getContext(), R.layout.create_new_team_dialog, null);
        final EditText editText = (EditText) rootView.findViewById(R.id.number);

        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    if (!TextUtils.isEmpty(editText.getText())) {
                        startScout(editText.getText().toString());
                    }
                }
                return false;
            }
        });

        return new AlertDialog.Builder(getActivity())
                .setView(rootView)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        if (!TextUtils.isEmpty(editText.getText())) {
                            startScout(editText.getText().toString());
                        } else {
                            // TODO: 07/31/2016 cancel dismiss of dialog or show snackbar
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create();
    }

    private void startScout(String teamNumber) {
        new Scout().createScoutId(teamNumber);
        startActivity(ScoutActivity.createIntent(getContext(), new Team(teamNumber, null)));
    }
}

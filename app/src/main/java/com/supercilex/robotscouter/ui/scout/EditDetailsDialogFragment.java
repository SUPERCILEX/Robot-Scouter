package com.supercilex.robotscouter.ui.scout;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.Team;

public class EditDetailsDialogFragment extends DialogFragment {
    private final static String TEAM_NUMBER = "number";
    private final static String TEAM_KEY = "key";
    private final static String TEAM_NAME = "name";
    private final static String TEAM_WEBSITE = "website";
    private final static String TEAM_LOGO = "logo";

    public static EditDetailsDialogFragment newInstance(String teamNumber,
                                                        String teamKey,
                                                        String teamName,
                                                        String teamWebsite,
                                                        String teamLogo) {
        EditDetailsDialogFragment f = new EditDetailsDialogFragment();

        Bundle args = new Bundle();
        args.putString(TEAM_NUMBER, teamNumber);
        args.putString(TEAM_KEY, teamKey);
        args.putString(TEAM_NAME, teamName);
        args.putString(TEAM_WEBSITE, teamWebsite);
        args.putString(TEAM_LOGO, teamLogo);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getDialog().getWindow()
                .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        final LayoutInflater inflater = getActivity().getLayoutInflater();
        @SuppressLint("InflateParams") final View view = inflater.inflate(R.layout.add_details_dialog,
                                                                          null);
        final EditText teamNickname = (EditText) view.findViewById(R.id.name);
        final EditText teamWebsite = (EditText) view.findViewById(R.id.website);
        final EditText teamLogo = (EditText) view.findViewById(R.id.logo_uri);

        teamNickname.setText(getArguments().getString(TEAM_NAME));
        teamWebsite.setText(getArguments().getString(TEAM_WEBSITE));
        teamLogo.setText(getArguments().getString(TEAM_LOGO));

//        teamWebsite.setOnEditorActionListener(new TextView.OnEditorActionListener() {
//            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
//                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
//                    sendBroadcast(teamNickname, teamWebsite);
//                }
//                return false;
//            }
//        });

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(view).setTitle(getString(R.string.team_details))
                // Add action buttons
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        String website = null;
                        String logo = null;

                        if (!TextUtils.isEmpty(teamWebsite.getText())) {
                            website = getUrl(teamWebsite.getText().toString());
                        }

                        if (!TextUtils.isEmpty(teamLogo.getText())) {
                            logo = getUrl(teamLogo.getText().toString());
                        }

                        new Team(teamNickname.getText().toString(),
                                 website,
                                 logo).updateTeamOverwrite(getArguments().getString(TEAM_NUMBER),
                                                           getArguments().getString(TEAM_KEY));
                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });
        return builder.create();
    }

    private String getUrl(String url) {
        if (url.contains("http://") || url.contains("https://")) {
            return url;
        } else {
            return "http://www." + url;
        }
    }
}

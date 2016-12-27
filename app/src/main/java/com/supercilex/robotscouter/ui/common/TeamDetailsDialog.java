package com.supercilex.robotscouter.ui.common;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.Team;

public class TeamDetailsDialog extends KeyboardDialog {
    private static final String TAG = "TeamDetailsDialog";

    private Team mTeam;
    private EditText mName;
    private EditText mWebsite;
    private EditText mMedia;

    public static void show(Team team, FragmentManager manager) {
        TeamDetailsDialog dialog = new TeamDetailsDialog();
        dialog.setArguments(team.getBundle());
        dialog.show(manager, TAG);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View rootView = View.inflate(getContext(), R.layout.dialog_edit_details, null);
        mTeam = Team.getTeam(getArguments());
        mName = (EditText) rootView.findViewById(R.id.name);
        mWebsite = (EditText) rootView.findViewById(R.id.website);
        mMedia = (EditText) rootView.findViewById(R.id.media);

        mName.setText(mTeam.getName());
        mWebsite.setText(mTeam.getWebsite());
        mMedia.setText(mTeam.getMedia());
        return createDialog(rootView, R.string.team_details);
    }

    @Override
    public boolean onClick() {
        if (isCustomDetail(mTeam.getName(), mName)) {
            mTeam.setHasCustomName(true);
            mTeam.setName(mName.getText().toString());
        }
        if (isCustomDetail(mTeam.getWebsite(), mWebsite)) {
            mTeam.setHasCustomWebsite(true);
            mTeam.setWebsite(formatUrl(mWebsite.getText().toString()));
        }
        if (isCustomDetail(mTeam.getMedia(), mMedia)) {
            mTeam.setHasCustomMedia(true);
            mTeam.setMedia(formatUrl(mMedia.getText().toString()));
        }
        mTeam.forceUpdate();
        return true;
    }

    private boolean isCustomDetail(String current, EditText possibleUpdate) {
        return !TextUtils.equals(current, possibleUpdate.getText().toString())
                && !TextUtils.isEmpty(possibleUpdate.getText());
    }

    private String formatUrl(String url) {
        if (url.contains("http://") || url.contains("https://")) {
            return url;
        } else {
            return "http://" + url;
        }
    }
}

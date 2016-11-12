package com.supercilex.robotscouter.ui.scout;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.ui.VisibleKeyboardDialog;
import com.supercilex.robotscouter.util.BaseHelper;

public class EditDetailsDialog extends VisibleKeyboardDialog {
    private Team mTeam;

    public static EditDetailsDialog newInstance(Team team) {
        EditDetailsDialog dialogFragment = new EditDetailsDialog();
        dialogFragment.setArguments(BaseHelper.getTeamBundle(team));
        return dialogFragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mTeam = mHelper.getTeam();

        // Get the layout inflater
        View rootView = View.inflate(getContext(), R.layout.add_details_dialog, null);
        final EditText name = (EditText) rootView.findViewById(R.id.name);
        final EditText website = (EditText) rootView.findViewById(R.id.website);
        final EditText media = (EditText) rootView.findViewById(R.id.media);

        name.setText(mTeam.getName());
        website.setText(mTeam.getWebsite());
        media.setText(mTeam.getMedia());

        return new AlertDialog.Builder(getActivity())
                .setView(rootView)
                .setTitle(getString(R.string.team_details))
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        if (isCustomDetail(mTeam.getName(), name)) {
                            mTeam.setHasCustomName(true);
                            mTeam.setName(name.getText().toString());
                        }
                        if (isCustomDetail(mTeam.getWebsite(), website)) {
                            mTeam.setHasCustomWebsite(true);
                            mTeam.setWebsite(getValidUrl(website.getText().toString()));
                        }
                        if (isCustomDetail(mTeam.getMedia(), media)) {
                            mTeam.setHasCustomMedia(true);
                            mTeam.setMedia(getValidUrl(media.getText().toString()));
                        }
                        mTeam.forceUpdate();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create();
    }

    private boolean isCustomDetail(String current, EditText possibleUpdate) {
        return !TextUtils.equals(current, possibleUpdate.getText().toString())
                && !TextUtils.isEmpty(possibleUpdate.getText());
    }

    private String getValidUrl(String url) {
        if (url.contains("http://") || url.contains("https://")) {
            return url;
        } else {
            return "http://" + url;
        }
    }
}

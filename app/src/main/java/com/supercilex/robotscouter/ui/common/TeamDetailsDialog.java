package com.supercilex.robotscouter.ui.common;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;

import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.Team;

public class TeamDetailsDialog extends KeyboardDialogBase implements View.OnFocusChangeListener {
    private static final String TAG = "TeamDetailsDialog";

    private Team mTeam;

    private TextInputLayout mNameInputLayout;
    private TextInputLayout mMediaInputLayout;
    private TextInputLayout mWebsiteInputLayout;
    private EditText mNameEditText;
    private EditText mMediaEditText;
    private EditText mWebsiteEditText;

    public static void show(Team team, FragmentManager manager) {
        TeamDetailsDialog dialog = new TeamDetailsDialog();
        dialog.setArguments(team.getBundle());
        dialog.show(manager, TAG);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mTeam = Team.getTeam(getArguments());

        View rootView = View.inflate(getContext(), R.layout.dialog_edit_details, null);
        mNameInputLayout = (TextInputLayout) rootView.findViewById(R.id.name_layout);
        mMediaInputLayout = (TextInputLayout) rootView.findViewById(R.id.media_layout);
        mWebsiteInputLayout = (TextInputLayout) rootView.findViewById(R.id.website_layout);
        mNameEditText = (EditText) rootView.findViewById(R.id.name);
        mMediaEditText = (EditText) rootView.findViewById(R.id.media);
        mWebsiteEditText = (EditText) rootView.findViewById(R.id.website);

        mNameEditText.setText(mTeam.getName());
        mMediaEditText.setText(mTeam.getMedia());
        mWebsiteEditText.setText(mTeam.getWebsite());
        mNameEditText.setOnFocusChangeListener(this);
        mMediaEditText.setOnFocusChangeListener(this);
        mWebsiteEditText.setOnFocusChangeListener(this);
        return createDialog(rootView, R.string.team_details);
    }

    @Override
    protected EditText getLastEditText() {
        return mMediaEditText;
    }

    @Override
    public boolean onClick() {
        boolean isNameValid = validateNotEmpty(mNameInputLayout);
        boolean isMediaValid = validateUrl(mMediaInputLayout);
        boolean isWebsiteValid = validateUrl(mWebsiteInputLayout);

        if (isNameValid && isWebsiteValid && isMediaValid) {
            String name = mNameEditText.getText().toString();
            if (!mTeam.getName().equals(name)) {
                mTeam.setHasCustomName(true);
                mTeam.setName(name);
            }

            String media = formatUrl(mMediaEditText.getText().toString());
            if (!mTeam.getMedia().equals(media)) {
                mTeam.setHasCustomMedia(true);
                mTeam.setMedia(media);
            }

            String website = formatUrl(mWebsiteEditText.getText().toString());
            if (!mTeam.getWebsite().equals(website)) {
                mTeam.setHasCustomWebsite(true);
                mTeam.setWebsite(website);
            }

            mTeam.forceUpdate();

            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) return; // Only consider views losing focus

        switch (v.getId()) {
            case R.id.name:
                validateNotEmpty(mNameInputLayout);
                break;
            case R.id.media:
                validateUrl(mMediaInputLayout);
                break;
            case R.id.website:
                validateUrl(mWebsiteInputLayout);
                break;
        }
    }

    private boolean validateNotEmpty(TextInputLayout inputLayout) {
        if (isEmpty(inputLayout)) {
            inputLayout.setError(getString(R.string.required_field));
            return false;
        } else {
            inputLayout.setError(null);
            return true;
        }
    }

    private boolean validateUrl(TextInputLayout inputLayout) {
        boolean isValid = !isEmpty(inputLayout)
                && Patterns.WEB_URL.matcher(formatUrl(inputLayout.getEditText()
                                                              .getText()
                                                              .toString())).matches();
        if (isValid) {
            inputLayout.setError(null);
            return true;
        } else {
            inputLayout.setError(getString(R.string.malformed_url));
            return false;
        }
    }

    private boolean isEmpty(TextInputLayout inputLayout) {
        return TextUtils.isEmpty(inputLayout.getEditText().getText());
    }

    private String formatUrl(String url) {
        url = url.trim();
        if (url.contains("http://") || url.contains("https://")) {
            return url;
        } else {
            return "http://" + url;
        }
    }
}

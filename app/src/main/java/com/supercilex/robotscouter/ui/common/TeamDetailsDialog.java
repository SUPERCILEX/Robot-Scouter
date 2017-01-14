package com.supercilex.robotscouter.ui.common;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;

import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.ui.teamlist.TeamListFragment;

public class TeamDetailsDialog extends KeyboardDialogBase implements View.OnFocusChangeListener {
    private static final String TAG = "TeamDetailsDialog";

    private Team mTeam;

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
        return mWebsiteEditText;
    }

    @Override
    public boolean onClick() {
        boolean isMediaValid = validateUrl(mMediaEditText.getText(), mMediaInputLayout);
        boolean isWebsiteValid = validateUrl(mWebsiteEditText.getText(), mWebsiteInputLayout);

        if (isWebsiteValid && isMediaValid) {
            String name = mNameEditText.getText().toString();
            if (mTeam.getName() == null ? !TextUtils.isEmpty(name) : !mTeam.getName()
                    .equals(name)) {
                mTeam.setHasCustomName();
                mTeam.setName(name);
            }

            String media = formatUrl(mMediaEditText.getText());
            if (mTeam.getMedia() == null ? !TextUtils.isEmpty(media) : !mTeam.getMedia()
                    .equals(media)) {
                mTeam.setHasCustomMedia();
                mTeam.setMedia(media);
            }

            String website = formatUrl(mWebsiteEditText.getText());
            if (mTeam.getWebsite() == null
                    ? !TextUtils.isEmpty(website) : !mTeam.getWebsite().equals(website)) {
                mTeam.setHasCustomWebsite();
                mTeam.setWebsite(website);
            }

            mTeam.forceUpdate();

            // If we are being called from TeamListFragment, reset the menu if the click was consumed
            Fragment fragment = getParentFragment();
            if (fragment instanceof TeamListFragment) {
                ((TeamListFragment) fragment).resetMenu();
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) return; // Only consider views losing focus

        switch (v.getId()) {
            case R.id.media:
                validateUrl(mMediaEditText.getText(), mMediaInputLayout);
                break;
            case R.id.website:
                validateUrl(mWebsiteEditText.getText(), mWebsiteInputLayout);
                break;
            default:
                break;
        }
    }

    private boolean validateUrl(CharSequence url, TextInputLayout inputLayout) {
        if (TextUtils.isEmpty(url)) return true;

        boolean isValid = Patterns.WEB_URL.matcher(formatUrl(url)).matches();
        if (isValid) {
            inputLayout.setError(null);
            return true;
        } else {
            inputLayout.setError(getString(R.string.malformed_url));
            return false;
        }
    }

    @Nullable
    private String formatUrl(CharSequence url) {
        String trimmedUrl = url.toString().trim();
        if (trimmedUrl.isEmpty()) return null;
        if (trimmedUrl.contains("http://") || trimmedUrl.contains("https://")) {
            return trimmedUrl;
        } else {
            return "http://" + trimmedUrl;
        }
    }
}

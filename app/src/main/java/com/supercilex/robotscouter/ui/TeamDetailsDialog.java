package com.supercilex.robotscouter.ui;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

import com.google.android.gms.tasks.OnSuccessListener;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.client.UploadTeamMediaJob;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.data.util.TeamHelper;
import com.supercilex.robotscouter.ui.teamlist.TeamListFragment;

import java.io.File;

public class TeamDetailsDialog extends KeyboardDialogBase implements View.OnFocusChangeListener, View.OnTouchListener, OnSuccessListener<Uri> {
    private static final String TAG = "TeamDetailsDialog";

    private TeamHelper mTeamHelper;
    private TeamMediaCreator mMediaCapture;

    private TextInputLayout mMediaInputLayout;
    private TextInputLayout mWebsiteInputLayout;
    private EditText mNameEditText;
    private EditText mMediaEditText;
    private EditText mWebsiteEditText;

    private float mStartX = -1;

    public static void show(FragmentManager manager, TeamHelper teamHelper) {
        TeamDetailsDialog dialog = new TeamDetailsDialog();
        dialog.setArguments(teamHelper.toBundle());
        dialog.show(manager, TAG);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTeamHelper = TeamHelper.parse(getArguments());
        if (savedInstanceState == null) {
            mMediaCapture = TeamMediaCreator.newInstance(this, mTeamHelper, this);
        } else {
            mMediaCapture = TeamMediaCreator.get(savedInstanceState, this, this);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View rootView = View.inflate(getContext(), R.layout.dialog_team_details, null);

        mMediaInputLayout = (TextInputLayout) rootView.findViewById(R.id.media_layout);
        mWebsiteInputLayout = (TextInputLayout) rootView.findViewById(R.id.website_layout);
        mNameEditText = (EditText) rootView.findViewById(R.id.name);
        mMediaEditText = (EditText) rootView.findViewById(R.id.media);
        mWebsiteEditText = (EditText) rootView.findViewById(R.id.website);

        mNameEditText.setText(mTeamHelper.getTeam().getName());
        mMediaEditText.setText(mTeamHelper.getTeam().getMedia());
        mWebsiteEditText.setText(mTeamHelper.getTeam().getWebsite());
        mNameEditText.setOnFocusChangeListener(this);
        mMediaEditText.setOnFocusChangeListener(this);
        mWebsiteEditText.setOnFocusChangeListener(this);

        mMediaEditText.setOnTouchListener(this);

        return createDialog(rootView, R.string.team_details);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putAll(mMediaCapture.toBundle());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected EditText getLastEditText() {
        return mWebsiteEditText;
    }

    @Override
    public boolean onClick() {
        boolean isMediaValid = validateUrl(mMediaEditText.getText().toString(), mMediaInputLayout);
        boolean isWebsiteValid = validateUrl(mWebsiteEditText.getText().toString(),
                                             mWebsiteInputLayout);

        if (isWebsiteValid && isMediaValid) {
            Team team = mTeamHelper.getTeam();

            String rawName = mNameEditText.getText().toString();
            String name = TextUtils.isEmpty(rawName) ? null : rawName;
            if (!TextUtils.equals(team.getName(), name)) {
                team.setHasCustomName(!TextUtils.isEmpty(name));
                team.setName(name);
            }

            String media = formatUrl(mMediaEditText.getText().toString());
            boolean isNewMedia = false;
            if (!TextUtils.equals(team.getMedia(), media)) {
                team.setHasCustomMedia(!TextUtils.isEmpty(media));
                team.setMedia(media);
                isNewMedia = true;
            }

            String website = formatUrl(mWebsiteEditText.getText().toString());
            if (!TextUtils.equals(team.getWebsite(), website)) {
                team.setHasCustomWebsite(!TextUtils.isEmpty(website));
                team.setWebsite(website);
            }

            mTeamHelper.forceUpdateTeam();

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
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction();
        float x = event.getX();

        if (action == MotionEvent.ACTION_DOWN && mStartX == -1) mStartX = x;

        int iconX = mMediaEditText.getRight() - mMediaEditText.getCompoundDrawables()[2].getBounds()
                .width();
        if (action == MotionEvent.ACTION_UP && mStartX >= iconX && x >= iconX) {
            mMediaCapture.capture();
            return true;
        }
        if (action == MotionEvent.ACTION_UP) mStartX = -1;

        return false;
    }

    /**
     * Used in {@link TeamMediaCreator#capture()}
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        mMediaCapture.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mMediaCapture.onActivityResult(requestCode, resultCode);
    }

    @Override
    public void onSuccess(Uri uri) {
        mMediaEditText.setText(uri.getPath());
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) return; // Only consider views losing focus

        int id = v.getId();
        if (id == R.id.media) {
            validateUrl(mMediaEditText.getText().toString(), mMediaInputLayout);
        } else if (id == R.id.website) {
            validateUrl(mWebsiteEditText.getText().toString(), mWebsiteInputLayout);
        }
    }

    private boolean validateUrl(String url, TextInputLayout inputLayout) {
        if (TextUtils.isEmpty(url)) return true;

        if (Patterns.WEB_URL.matcher(formatUrl(url)).matches() || new File(url).exists()) {
            inputLayout.setError(null);
            return true;
        } else {
            inputLayout.setError(getString(R.string.malformed_url));
            return false;
        }
    }

    @Nullable
    private String formatUrl(String url) {
        if (new File(url).exists()) return url;

        String trimmedUrl = url.trim();
        if (trimmedUrl.isEmpty()) return null;
        if (trimmedUrl.contains("http://") || trimmedUrl.contains("https://")) {
            return trimmedUrl;
        } else {
            return "http://" + trimmedUrl;
        }
    }
}

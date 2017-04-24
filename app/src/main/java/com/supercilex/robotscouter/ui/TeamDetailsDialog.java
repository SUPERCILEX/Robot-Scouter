package com.supercilex.robotscouter.ui;

import android.app.Dialog;
import android.content.Intent;
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

import com.firebase.ui.database.ChangeEventListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.data.util.TeamHelper;
import com.supercilex.robotscouter.ui.teamlist.TeamListFragment;
import com.supercilex.robotscouter.util.Constants;

import java.io.File;

public class TeamDetailsDialog extends KeyboardDialogBase
        implements View.OnFocusChangeListener, View.OnTouchListener, OnSuccessListener<TeamHelper>, TeamMediaCreator.StartCaptureListener, ChangeEventListener {
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
        dialog.setArguments(new Team.Builder(teamHelper.getTeam()).build().getHelper().toBundle());
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

        Constants.sFirebaseTeams.addChangeEventListener(this);
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

        mNameEditText.setOnFocusChangeListener(this);
        mMediaEditText.setOnFocusChangeListener(this);
        mWebsiteEditText.setOnFocusChangeListener(this);
        mMediaEditText.setOnTouchListener(this);

        updateUi();

        return createDialog(rootView, R.string.team_details);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putAll(mMediaCapture.toBundle());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Constants.sFirebaseTeams.removeChangeEventListener(this);
    }

    private void updateUi() {
        if (mNameEditText != null && mMediaCapture != null && mWebsiteEditText != null) {
            mNameEditText.setText(mTeamHelper.getTeam().getName());
            mMediaEditText.setText(mTeamHelper.getTeam().getMedia());
            mWebsiteEditText.setText(mTeamHelper.getTeam().getWebsite());
        }
    }

    @Override
    protected EditText getLastEditText() {
        return mWebsiteEditText;
    }

    @Override
    public void onChildChanged(EventType type, DataSnapshot snapshot, int index, int oldIndex) {
        if (!TextUtils.equals(mTeamHelper.getTeam().getKey(), snapshot.getKey())) return;

        if (type == EventType.CHANGED) {
            mTeamHelper =
                    new Team.Builder(Constants.sFirebaseTeams.getObject(index)).build().getHelper();
            mMediaCapture.setTeamHelper(mTeamHelper);
            updateUi();
        } else if (type == EventType.REMOVED) {
            dismiss();
        }
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
            if (!TextUtils.equals(team.getMedia(), media)) {
                team.setHasCustomMedia(!TextUtils.isEmpty(media));
                team.setMedia(media);
            }

            String website = formatUrl(mWebsiteEditText.getText().toString());
            if (!TextUtils.equals(team.getWebsite(), website)) {
                team.setHasCustomWebsite(!TextUtils.isEmpty(website));
                team.setWebsite(website);
            }

            mTeamHelper.forceUpdateTeam();
            mTeamHelper.forceRefresh();

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
            ShouldUploadMediaToTbaDialog.show(this);
            return true;
        }
        if (action == MotionEvent.ACTION_UP) mStartX = -1;

        return false;
    }

    /**
     * Used in {@link TeamMediaCreator#startCapture(boolean)}
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
    public void onSuccess(TeamHelper teamHelper) {
        mTeamHelper.copyMediaInfo(teamHelper);
        updateUi();
    }

    @Override
    public void onStartCapture(boolean shouldUploadMediaToTba) {
        mMediaCapture.startCapture(shouldUploadMediaToTba);
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
    private String formatUrl(@NonNull String url) {
        if (new File(url).exists()) return url;

        String trimmedUrl = url.trim();
        if (trimmedUrl.isEmpty()) return null;
        if (trimmedUrl.contains("http://") || trimmedUrl.contains("https://")) {
            return trimmedUrl;
        } else {
            return "http://" + trimmedUrl;
        }
    }

    @Override
    public void onCancelled(DatabaseError error) {
        FirebaseCrash.report(error.toException());
    }

    @Override
    public void onDataChanged() {
        // Noop
    }
}

package com.supercilex.robotscouter.ui;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.app.Dialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.transition.TransitionManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.tasks.OnSuccessListener;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.ui.teamlist.TeamListFragment;
import com.supercilex.robotscouter.util.data.ArgUtilsKt;
import com.supercilex.robotscouter.util.data.model.TeamHolder;
import com.supercilex.robotscouter.util.data.model.TeamUtilsKt;
import com.supercilex.robotscouter.util.ui.KeyboardDialogBase;
import com.supercilex.robotscouter.util.ui.TeamMediaCreator;
import com.supercilex.robotscouter.util.ui.views.ContentLoadingProgressBar;

import java.io.File;

import static com.supercilex.robotscouter.util.AnalyticsUtilsKt.logEditDetails;
import static com.supercilex.robotscouter.util.ui.ViewUtilsKt.animateCircularReveal;

public class TeamDetailsDialog extends KeyboardDialogBase
        implements View.OnClickListener, View.OnFocusChangeListener,
        OnSuccessListener<Team>, TeamMediaCreator.StartCaptureListener {
    private static final String TAG = "TeamDetailsDialog";

    private Team mTeam;
    private TeamMediaCreator mMediaCapture;

    private ViewGroup mRootView;

    private ImageView mMedia;
    private ContentLoadingProgressBar mMediaLoadProgress;
    private TextView mName;
    private ImageButton mEditNameButton;

    private TextInputLayout mNameInputLayout;
    private TextInputLayout mMediaInputLayout;
    private TextInputLayout mWebsiteInputLayout;
    private EditText mNameEditText;
    private EditText mMediaEditText;
    private EditText mWebsiteEditText;

    public static void show(FragmentManager manager, Team team) {
        TeamDetailsDialog dialog = new TeamDetailsDialog();
        dialog.setArguments(ArgUtilsKt.toBundle(new Team(
                team.getNumber(),
                team.getId(),
                team.getOwners(),
                team.getActiveTokens(),
                team.getPendingApprovals(),
                team.getTemplateId(),
                team.getName(),
                team.getMedia(),
                team.getWebsite(),
                team.getHasCustomName(),
                team.getHasCustomMedia(),
                team.getHasCustomWebsite(),
                team.getShouldUploadMediaToTba(),
                team.getMediaYear(),
                team.getTimestamp())));
        dialog.show(manager, TAG);

        logEditDetails(team);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTeam = ArgUtilsKt.getTeam(getArguments());

        if (savedInstanceState == null) {
            mMediaCapture = TeamMediaCreator.newInstance(this, mTeam, this);
        } else {
            mMediaCapture = TeamMediaCreator.get(savedInstanceState, this, this);
        }

        TeamHolder holder = ViewModelProviders.of(this).get(TeamHolder.class);
        holder.init(getArguments());
        holder.getTeamListener().observe(this, team -> {
            if (team == null) {
                dismiss();
            } else {
                mTeam = team;
                mMediaCapture.setTeam(mTeam);
                updateUi();
            }
        });
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mRootView = (ViewGroup) View.inflate(getContext(), R.layout.dialog_team_details, null);

        mMedia = mRootView.findViewById(R.id.media);
        mMediaLoadProgress = mRootView.findViewById(R.id.progress);
        mName = mRootView.findViewById(R.id.name);
        mEditNameButton = mRootView.findViewById(R.id.edit_name_button);

        mNameInputLayout = mRootView.findViewById(R.id.name_layout);
        mMediaInputLayout = mRootView.findViewById(R.id.media_layout);
        mWebsiteInputLayout = mRootView.findViewById(R.id.website_layout);
        mNameEditText = mRootView.findViewById(R.id.name_edit);
        mMediaEditText = mRootView.findViewById(R.id.media_edit);
        mWebsiteEditText = mRootView.findViewById(R.id.website_edit);

        mMedia.setOnClickListener(v -> ShouldUploadMediaToTbaDialog.Companion.show(this));
        mEditNameButton.setOnClickListener(this);

        mNameEditText.setOnFocusChangeListener(this);
        mMediaEditText.setOnFocusChangeListener(this);
        mWebsiteEditText.setOnFocusChangeListener(this);

        updateUi();

        return createDialog(mRootView, R.string.details_title, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getDialog().getWindow()
                .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putAll(mMediaCapture.toBundle());
    }

    private void updateUi() {
        if (mMedia != null
                && mName != null
                && mNameEditText != null
                && mMediaCapture != null
                && mWebsiteEditText != null) {
            TransitionManager.beginDelayedTransition(mRootView);

            mMediaLoadProgress.show(null);
            Glide.with(getContext())
                    .load(mTeam.getMedia())
                    .apply(RequestOptions.circleCropTransform()
                                   .error(R.drawable.ic_person_grey_96dp))
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onResourceReady(Drawable resource,
                                                       Object model,
                                                       Target<Drawable> target,
                                                       DataSource dataSource,
                                                       boolean isFirstResource) {
                            mMediaLoadProgress.hide(true, null);
                            return false;
                        }

                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e,
                                                    Object model,
                                                    Target<Drawable> target,
                                                    boolean isFirstResource) {
                            mMediaLoadProgress.hide(true, null);
                            return false;
                        }
                    })
                    .into(mMedia);
            mName.setText(mTeam.toString());

            mNameEditText.setText(mTeam.getName());
            mMediaEditText.setText(mTeam.getMedia());
            mWebsiteEditText.setText(mTeam.getWebsite());
        }
    }

    @NonNull
    @Override
    protected EditText getLastEditText() {
        return mWebsiteEditText;
    }

    @Override
    public void onClick(View v) {
        Animator editNameAnimator = animateCircularReveal(
                mEditNameButton,
                false,
                0,
                mEditNameButton.getHeight() / 2,
                mEditNameButton.getWidth());
        Animator nameAnimator = animateCircularReveal(
                mName,
                false,
                0,
                mName.getHeight() / 2,
                mName.getWidth());

        int buttonCenterX = mEditNameButton.getLeft() + (mEditNameButton.getWidth() / 2);
        Animator nameLayoutAnimator = animateCircularReveal(
                mNameInputLayout,
                true,
                buttonCenterX,
                0,
                (float) Math.hypot(buttonCenterX, mNameInputLayout.getHeight()));

        if (editNameAnimator != null && nameAnimator != null && nameLayoutAnimator != null) {
            AnimatorSet animator = new AnimatorSet();
            animator.playTogether(editNameAnimator, nameAnimator, nameLayoutAnimator);
            animator.start();
        }
    }

    @Override
    public boolean onAttemptDismiss() {
        boolean isMediaValid = validateUrl(mMediaEditText.getText().toString(), mMediaInputLayout);
        boolean isWebsiteValid = validateUrl(mWebsiteEditText.getText().toString(),
                                             mWebsiteInputLayout);

        if (isWebsiteValid && isMediaValid) {
            String rawName = mNameEditText.getText().toString();
            String name = TextUtils.isEmpty(rawName) ? null : rawName;
            if (!TextUtils.equals(mTeam.getName(), name)) {
                mTeam.setHasCustomName(!TextUtils.isEmpty(name));
                mTeam.setName(name);
            }

            String media = formatUrl(mMediaEditText.getText().toString());
            if (!TextUtils.equals(mTeam.getMedia(), media)) {
                mTeam.setHasCustomMedia(!TextUtils.isEmpty(media));
                mTeam.setMedia(media);
            }

            String website = formatUrl(mWebsiteEditText.getText().toString());
            if (!TextUtils.equals(mTeam.getWebsite(), website)) {
                mTeam.setHasCustomWebsite(!TextUtils.isEmpty(website));
                mTeam.setWebsite(website);
            }

            TeamUtilsKt.forceUpdate(mTeam);
            TeamUtilsKt.forceRefresh(mTeam);

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
    public void onSuccess(Team team) {
        TeamUtilsKt.copyMediaInfo(mTeam, team);
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
        } else if (id == R.id.website_edit) {
            validateUrl(mWebsiteEditText.getText().toString(), mWebsiteInputLayout);
        }
    }

    private boolean validateUrl(String url, TextInputLayout inputLayout) {
        if (TextUtils.isEmpty(url)) return true;

        if (Patterns.WEB_URL.matcher(formatUrl(url)).matches() || new File(url).exists()) {
            inputLayout.setError(null);
            return true;
        } else {
            inputLayout.setError(getString(R.string.details_malformed_url_error));
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
}

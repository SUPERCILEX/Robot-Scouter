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
import android.widget.ProgressBar;
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
import com.supercilex.robotscouter.data.util.TeamHelper;
import com.supercilex.robotscouter.ui.teamlist.TeamListFragment;

import java.io.File;

import static com.supercilex.robotscouter.util.ViewUtilsKt.animateCircularReveal;

public class TeamDetailsDialog extends KeyboardDialogBase
        implements View.OnClickListener, View.OnFocusChangeListener,
        OnSuccessListener<TeamHelper>, TeamMediaCreator.StartCaptureListener {
    private static final String TAG = "TeamDetailsDialog";

    private TeamHelper mTeamHelper;
    private TeamMediaCreator mMediaCapture;

    private ViewGroup mRootView;

    private ImageView mMedia;
    private ProgressBar mMediaLoadProgress;
    private TextView mName;
    private ImageButton mEditNameButton;

    private TextInputLayout mNameInputLayout;
    private TextInputLayout mMediaInputLayout;
    private TextInputLayout mWebsiteInputLayout;
    private EditText mNameEditText;
    private EditText mMediaEditText;
    private EditText mWebsiteEditText;

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

        TeamHolder holder = ViewModelProviders.of(this).get(TeamHolder.class);
        holder.init(getArguments());
        holder.getTeamHelperListener().observe(this, helper -> {
            if (helper == null) {
                dismiss();
            } else {
                mTeamHelper = helper;
                mMediaCapture.setTeamHelper(mTeamHelper);
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

        return createDialog(mRootView, R.string.team_details);
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

            Team team = mTeamHelper.getTeam();

            mMediaLoadProgress.setVisibility(View.VISIBLE);
            Glide.with(getContext())
                    .load(team.getMedia())
                    .apply(RequestOptions.circleCropTransform()
                                   .error(R.drawable.ic_memory_grey_48dp))
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onResourceReady(Drawable resource,
                                                       Object model,
                                                       Target<Drawable> target,
                                                       DataSource dataSource,
                                                       boolean isFirstResource) {
                            mMediaLoadProgress.setVisibility(View.GONE);
                            return false;
                        }

                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e,
                                                    Object model,
                                                    Target<Drawable> target,
                                                    boolean isFirstResource) {
                            mMediaLoadProgress.setVisibility(View.GONE);
                            return false;
                        }
                    })
                    .into(mMedia);
            mName.setText(team.toString());

            mNameEditText.setText(team.getName());
            mMediaEditText.setText(team.getMedia());
            mWebsiteEditText.setText(team.getWebsite());
        }
    }

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
}

package com.supercilex.robotscouter.ui.scout;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.util.TeamHelper;
import com.supercilex.robotscouter.ui.ShouldUploadMediaToTbaDialog;
import com.supercilex.robotscouter.ui.TeamMediaCreator;

public abstract class AppBarViewHolderBase
        implements OnSuccessListener<Void>, View.OnLongClickListener, TeamMediaCreator.StartCaptureListener, ActivityCompat.OnRequestPermissionsResultCallback {
    private final Fragment mFragment;
    protected final Toolbar mToolbar;
    protected final CollapsingToolbarLayout mHeader;
    private final ImageView mBackdrop;

    private final TaskCompletionSource<Void> mOnMenuReadyTask = new TaskCompletionSource<>();
    private final Task mOnScoutingReadyTask;

    protected TeamHelper mTeamHelper;

    private TeamMediaCreator mMediaCapture;
    private final OnSuccessListener<TeamHelper> mMediaCaptureListener = teamHelper -> {
        mTeamHelper.copyMediaInfo(teamHelper);
        mTeamHelper.forceUpdateTeam();
    };

    private MenuItem mNewScoutItem;
    private MenuItem mAddMediaItem;
    private MenuItem mVisitTeamWebsiteItem;
    private MenuItem mDeleteScoutItem;
    private boolean mIsDeleteScoutItemVisible;

    private boolean mInit;

    protected AppBarViewHolderBase(TeamHelper teamHelper,
                                   Fragment fragment,
                                   Task onScoutingReadyTask) {
        mTeamHelper = teamHelper;
        mFragment = fragment;
        View rootView = fragment.getView();
        mToolbar = (Toolbar) rootView.findViewById(R.id.toolbar);
        mHeader = (CollapsingToolbarLayout) rootView.findViewById(R.id.header);
        mBackdrop = (ImageView) rootView.findViewById(R.id.backdrop);
        mOnScoutingReadyTask = onScoutingReadyTask;
        mMediaCapture = TeamMediaCreator.newInstance(mFragment, mTeamHelper, mMediaCaptureListener);

        mBackdrop.setOnLongClickListener(this);
    }

    public final void bind(@NonNull TeamHelper teamHelper) {
        if (!mTeamHelper.equals(teamHelper) || !mInit) {
            mTeamHelper = teamHelper;
            bind();
        }
        mInit = true;
    }

    @CallSuper
    protected void bind() {
        mToolbar.setTitle(mTeamHelper.toString());
        mMediaCapture.setTeamHelper(mTeamHelper);
        loadImages();
        bindMenu();
    }

    private void loadImages() {
        String media = mTeamHelper.getTeam().getMedia();
        Glide.with(mFragment)
                .load(media)
                .asBitmap()
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .error(R.drawable.ic_memory_grey_144dp)
                .listener(new RequestListener<String, Bitmap>() {
                    @Override
                    public boolean onException(Exception e,
                                               String model,
                                               Target<Bitmap> target,
                                               boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Bitmap resource,
                                                   String model,
                                                   Target<Bitmap> target,
                                                   boolean isFromMemoryCache,
                                                   boolean isFirstResource) {
                        if (resource != null && !resource.isRecycled()) {
                            Palette.from(resource).generate(palette -> {
                                Palette.Swatch vibrantSwatch = palette.getVibrantSwatch();
                                if (vibrantSwatch != null) {
                                    updateScrim(vibrantSwatch.getRgb(), resource);
                                    return;
                                }

                                Palette.Swatch dominantSwatch = palette.getDominantSwatch();
                                if (dominantSwatch != null) {
                                    updateScrim(dominantSwatch.getRgb(), resource);
                                }
                            });
                        }
                        return false;
                    }
                })
                .into(mBackdrop);
    }

    @CallSuper
    protected void updateScrim(@ColorInt int color, Bitmap bitmap) {
        mHeader.setContentScrimColor(getTransparentColor(color));
    }

    public final void initMenu(Menu menu) {
        mNewScoutItem = menu.findItem(R.id.action_new_scout);
        mAddMediaItem = menu.findItem(R.id.action_add_media);
        mVisitTeamWebsiteItem = menu.findItem(R.id.action_visit_team_website);
        mDeleteScoutItem = menu.findItem(R.id.action_delete);

        menu.findItem(R.id.action_visit_tba_team_website)
                .setTitle(mFragment.getString(R.string.visit_team_website_on_tba,
                                              mTeamHelper.getTeam().getNumber()));
        mVisitTeamWebsiteItem.setTitle(mFragment.getString(R.string.visit_team_website,
                                                           mTeamHelper.getTeam().getNumber()));
        if (!mOnScoutingReadyTask.isComplete()) mNewScoutItem.setVisible(false);

        mOnMenuReadyTask.trySetResult(null);
        bindMenu();
    }

    public void setDeleteScoutMenuItemVisible(boolean visible) {
        mIsDeleteScoutItemVisible = visible;
        bindMenu();
    }

    private void bindMenu() {
        Tasks.whenAll(mOnMenuReadyTask.getTask(), mOnScoutingReadyTask).addOnSuccessListener(this);
    }

    @Override
    public final void onSuccess(Void aVoid) {
        mNewScoutItem.setVisible(true);
        mAddMediaItem.setVisible(mTeamHelper.isOutdatedMedia());
        mVisitTeamWebsiteItem.setVisible(!TextUtils.isEmpty(mTeamHelper.getTeam().getWebsite()));
        mDeleteScoutItem.setVisible(mIsDeleteScoutItemVisible);
    }

    private int getTransparentColor(@ColorInt int opaque) {
        return Color.argb(Math.round(Color.alpha(opaque) * 0.6f),
                          Color.red(opaque),
                          Color.green(opaque),
                          Color.blue(opaque));
    }

    @Override
    public boolean onLongClick(View v) {
        if (v.getId() == R.id.backdrop) {
            ShouldUploadMediaToTbaDialog.show(mFragment);
            return true;
        }
        return false;
    }

    @Override
    public void onStartCapture(boolean shouldUploadMediaToTba) {
        mMediaCapture.startCapture(shouldUploadMediaToTba);
    }

    @CallSuper
    public void onActivityResult(int requestCode, int resultCode) {
        mMediaCapture.onActivityResult(requestCode, resultCode);
    }

    @CallSuper
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        mMediaCapture.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @CallSuper
    public void onSaveInstanceState(Bundle outState) {
        outState.putAll(mMediaCapture.toBundle());
    }

    @CallSuper
    public void restoreState(Bundle savedInstanceState) {
        mMediaCapture = TeamMediaCreator.get(savedInstanceState, mFragment, mMediaCaptureListener);
    }
}

package com.supercilex.robotscouter.ui.scout;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.design.widget.CollapsingToolbarLayout;
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
import com.supercilex.robotscouter.ui.TeamMediaCreator;

public abstract class AppBarViewHolderBase implements OnSuccessListener<Void>, View.OnClickListener {
    private boolean mInit;

    protected TeamHelper mTeamHelper;
    private Fragment mFragment;

    protected Toolbar mToolbar;
    protected CollapsingToolbarLayout mHeader;
    private ImageView mBackdrop;

    private MenuItem mVisitTeamWebsiteItem;
    private MenuItem mNewScoutItem;
    private MenuItem mDeleteScoutItem;
    private TaskCompletionSource<Void> mOnMenuReadyTask = new TaskCompletionSource<>();
    private Task mOnScoutingReadyTask;
    private boolean mIsDeleteScoutItemVisible;

    private TeamMediaCreator mMediaCapture;
    private OnSuccessListener<Uri> mMediaCaptureListener = uri -> {
        mTeamHelper.getTeam().setHasCustomMedia(true);
        mTeamHelper.getTeam().setMedia(uri.getPath());
        mTeamHelper.forceUpdateTeam();
    };

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
        mBackdrop.setOnClickListener(this);

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
                .error(R.drawable.ic_add_a_photo_black_144dp)
                .listener(new RequestListener<String, Bitmap>() {
                    @Override
                    public boolean onException(Exception e,
                                               String s,
                                               Target<Bitmap> target,
                                               boolean b) {
                        mBackdrop.setOnClickListener(AppBarViewHolderBase.this);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Bitmap bitmap,
                                                   String s,
                                                   Target<Bitmap> target,
                                                   boolean b,
                                                   boolean b1) {
                        mBackdrop.setOnClickListener(null);

                        if (bitmap != null && !bitmap.isRecycled()) {
                            Palette.from(bitmap).generate(palette -> {
                                Palette.Swatch vibrantSwatch = palette.getVibrantSwatch();
                                if (vibrantSwatch != null) {
                                    updateScrim(vibrantSwatch.getRgb(), bitmap);
                                    return;
                                }

                                Palette.Swatch dominantSwatch = palette.getDominantSwatch();
                                if (dominantSwatch != null) {
                                    updateScrim(dominantSwatch.getRgb(), bitmap);
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
        mVisitTeamWebsiteItem = menu.findItem(R.id.action_visit_team_website);
        mNewScoutItem = menu.findItem(R.id.action_new_scout);
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
        if (mVisitTeamWebsiteItem != null) {
            mVisitTeamWebsiteItem.setVisible(
                    !TextUtils.isEmpty(mTeamHelper.getTeam().getWebsite()));
        }
        Tasks.whenAll(mOnMenuReadyTask.getTask(), mOnScoutingReadyTask).addOnSuccessListener(this);
    }

    @Override
    public final void onSuccess(Void aVoid) {
        mNewScoutItem.setVisible(true);
        mDeleteScoutItem.setVisible(mIsDeleteScoutItemVisible);
    }

    private int getTransparentColor(@ColorInt int opaque) {
        return Color.argb(Math.round(Color.alpha(opaque) * 0.6f),
                          Color.red(opaque),
                          Color.green(opaque),
                          Color.blue(opaque));
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.backdrop) mMediaCapture.capture();
    }

    @CallSuper
    public void onActivityResult(int requestCode, int resultCode) {
        mMediaCapture.onActivityResult(requestCode, resultCode);
    }

    @CallSuper
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) { // NOPMD
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

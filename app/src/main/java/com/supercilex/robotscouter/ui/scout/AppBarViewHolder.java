package com.supercilex.robotscouter.ui.scout;

import android.graphics.Bitmap;
import android.graphics.Color;
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
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.util.TeamHelper;

public abstract class AppBarViewHolder implements OnSuccessListener<Void> {
    protected TeamHelper mTeamHelper;
    private Fragment mFragment;

    protected Toolbar mToolbar;
    protected CollapsingToolbarLayout mHeader;
    private ImageView mBackdrop;

    private MenuItem mVisitTeamWebsiteItem;
    private MenuItem mNewScoutItem;
    private TaskCompletionSource<Void> mOnMenuReadyTask = new TaskCompletionSource<>();
    private Task mOnScoutingReadyTask;

    protected AppBarViewHolder(TeamHelper teamHelper, Fragment fragment, Task onScoutingReadyTask) {
        mTeamHelper = teamHelper;
        mFragment = fragment;
        View rootView = fragment.getView();
        mToolbar = (Toolbar) rootView.findViewById(R.id.toolbar);
        mHeader = (CollapsingToolbarLayout) rootView.findViewById(R.id.header);
        mBackdrop = (ImageView) rootView.findViewById(R.id.backdrop);
        mOnScoutingReadyTask = onScoutingReadyTask;

        bind();
    }

    public final void bind(@NonNull TeamHelper teamHelper) {
        if (!mTeamHelper.equals(teamHelper)) {
            mTeamHelper = teamHelper;
            bind();
        }
    }

    @CallSuper
    protected void bind() {
        mToolbar.setTitle(mTeamHelper.toString());
        loadImages();
        bindMenu();
    }

    private void loadImages() {
        String media = mTeamHelper.getTeam().getMedia();
        Glide.with(mFragment)
                .load(media)
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .error(R.drawable.ic_android_black_144dp)
                .into(mBackdrop);

        Glide.with(mFragment)
                .load(media)
                .asBitmap()
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(final Bitmap bitmap,
                                                GlideAnimation glideAnimation) {
                        if (bitmap != null && !bitmap.isRecycled()) {
                            Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
                                @Override
                                public void onGenerated(Palette palette) {
                                    Palette.Swatch vibrantSwatch = palette.getVibrantSwatch();
                                    if (vibrantSwatch != null) {
                                        updateScrim(vibrantSwatch.getRgb(), bitmap);
                                    }
                                }
                            });
                        }
                    }
                });
    }

    @CallSuper
    protected void updateScrim(@ColorInt int color, Bitmap bitmap) {
        mHeader.setContentScrimColor(getTransparentColor(color));
    }

    public final void initMenu(Menu menu) {
        mVisitTeamWebsiteItem = menu.findItem(R.id.action_visit_team_website);
        mNewScoutItem = menu.findItem(R.id.action_new_scout);

        menu.findItem(R.id.action_visit_tba_team_website)
                .setTitle(mFragment.getString(R.string.visit_team_website_on_tba,
                                              mTeamHelper.getTeam().getNumber()));
        mVisitTeamWebsiteItem.setTitle(mFragment.getString(R.string.visit_team_website,
                                                           mTeamHelper.getTeam().getNumber()));
        if (!mOnScoutingReadyTask.isComplete()) mNewScoutItem.setVisible(false);

        mOnMenuReadyTask.trySetResult(null);
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
    }

    private int getTransparentColor(@ColorInt int opaque) {
        return Color.argb(Math.round(Color.alpha(opaque) * 0.6f),
                          Color.red(opaque),
                          Color.green(opaque),
                          Color.blue(opaque));
    }
}

package com.supercilex.robotscouter.ui.scout;

import android.app.ActivityManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.util.TeamHelper;

public class AppBarViewHolder {
    private TeamHelper mTeamHelper;

    private AppCompatActivity mActivity;
    private CollapsingToolbarLayout mHeader;
    private ImageView mBackdrop;

    private MenuItem mActionVisitTeamWebsite;

    public AppBarViewHolder(AppCompatActivity activity) {
        mActivity = activity;
        mHeader = (CollapsingToolbarLayout) activity.findViewById(R.id.header);
        mBackdrop = (ImageView) activity.findViewById(R.id.backdrop);
    }

    public void bind(@NonNull TeamHelper teamHelper) {
        if (mTeamHelper != null && mTeamHelper.equals(teamHelper)) return;
        mTeamHelper = teamHelper;
        mActivity.getSupportActionBar().setTitle(mTeamHelper.getFormattedName());
        setTaskDescription(null, ContextCompat.getColor(mActivity, R.color.colorPrimary));
        loadImages();
        bindMenu();
    }

    private void loadImages() {
        Glide.with(mActivity)
                .load(mTeamHelper.getTeam().getMedia())
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .error(R.drawable.ic_android_black_144dp)
                .into(mBackdrop);

        Glide.with(mActivity)
                .load(mTeamHelper.getTeam().getMedia())
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
                                        int opaque = vibrantSwatch.getRgb();
                                        mHeader.setStatusBarScrimColor(opaque);
                                        mHeader.setContentScrimColor(getTransparentColor(opaque));
                                        setTaskDescription(bitmap, opaque);
                                    }
                                }
                            });
                        }
                    }
                });
    }

    private void setTaskDescription(Bitmap icon, @ColorInt int colorPrimary) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mActivity.setTaskDescription(
                    new ActivityManager.TaskDescription(mTeamHelper.getFormattedName(),
                                                        icon,
                                                        colorPrimary));
        }
    }

    public void initMenu(Menu menu) {
        menu.findItem(R.id.action_visit_tba_team_website)
                .setTitle(mActivity.getString(R.string.visit_team_website_on_tba,
                                              mTeamHelper.getTeam().getNumber()));

        mActionVisitTeamWebsite = menu.findItem(R.id.action_visit_team_website);
        mActionVisitTeamWebsite.setTitle(mActivity.getString(R.string.visit_team_website,
                                                             mTeamHelper.getTeam().getNumber()));
        bindMenu();
    }

    private void bindMenu() {
        if (mActionVisitTeamWebsite != null) {
            mActionVisitTeamWebsite.setVisible(
                    !TextUtils.isEmpty(mTeamHelper.getTeam().getWebsite()));
        }
    }

    private int getTransparentColor(@ColorInt int opaque) {
        return Color.argb(Math.round(Color.alpha(opaque) * 0.6f),
                          Color.red(opaque),
                          Color.green(opaque),
                          Color.blue(opaque));
    }
}

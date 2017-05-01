package com.supercilex.robotscouter.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.ColorRes;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewAnimationUtils;

public enum ViewUtils {
    INSTANCE;

    private Resources mResources;
    private int mDefaultAnimationDuration;

    public static void init(Context context) {
        INSTANCE.mResources = context.getResources();
        INSTANCE.mDefaultAnimationDuration =
                INSTANCE.mResources.getInteger(android.R.integer.config_mediumAnimTime);
    }

    public static boolean isTabletMode() {
        Configuration config = INSTANCE.mResources.getConfiguration();
        int size = config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        return size == Configuration.SCREENLAYOUT_SIZE_LARGE
                && config.orientation == Configuration.ORIENTATION_LANDSCAPE
                || size > Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public static void animateColorChange(Context context,
                                          @ColorRes int from,
                                          @ColorRes int to,
                                          ValueAnimator.AnimatorUpdateListener listener) {
        animateColorChange(context, from, to, INSTANCE.mDefaultAnimationDuration, listener);
    }

    public static void animateColorChange(Context context,
                                          @ColorRes int from,
                                          @ColorRes int to,
                                          int duration,
                                          ValueAnimator.AnimatorUpdateListener listener) {
        ValueAnimator animator = ValueAnimator.ofObject(
                new ArgbEvaluator(),
                ContextCompat.getColor(context, from),
                ContextCompat.getColor(context, to));
        animator.setDuration(duration);
        animator.addUpdateListener(listener);
        animator.start();
    }

    public static void animateCircularReveal(View view, boolean visible) {
        if (visible && view.getVisibility() == View.VISIBLE
                || !visible && view.getVisibility() == View.GONE) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (!view.isAttachedToWindow()) {
                view.setVisibility(visible ? View.VISIBLE : View.GONE);
                return;
            }

            int centerX = view.getWidth() / 2;
            int centerY = view.getHeight() / 2;
            float radius = (float) Math.hypot(centerX, centerY);
            Animator anim = ViewAnimationUtils.createCircularReveal(
                    view,
                    centerX,
                    centerY,
                    visible ? 0 : radius,
                    visible ? radius : 0);

            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (!visible) view.setVisibility(View.GONE);
                }
            });
            if (visible) view.setVisibility(View.VISIBLE);

            anim.start();
        } else {
            view.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }
}

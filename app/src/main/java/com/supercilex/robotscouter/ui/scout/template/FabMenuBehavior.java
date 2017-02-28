package com.supercilex.robotscouter.ui.scout.template;

import android.content.Context;
import android.support.annotation.Keep;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;

import com.github.clans.fab.FloatingActionMenu;

public class FabMenuBehavior extends CoordinatorLayout.Behavior<FloatingActionMenu> {
    @Keep
    public FabMenuBehavior() {
        super();
    }

    @Keep
    public FabMenuBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent,
                                   FloatingActionMenu child,
                                   View dependency) {
        return dependency instanceof Snackbar.SnackbarLayout;
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent,
                                          FloatingActionMenu child,
                                          View dependency) {
        float translationY =
                Math.min(0, ViewCompat.getTranslationY(dependency) - dependency.getHeight());
        ViewCompat.setTranslationY(child, translationY);
        return true;
    }

    @Override
    public void onDependentViewRemoved(CoordinatorLayout parent,
                                       FloatingActionMenu child,
                                       View dependency) {
        float translationY =
                Math.max(0, ViewCompat.getTranslationY(dependency) - dependency.getHeight());
        ViewCompat.setTranslationY(child, translationY);
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout,
                                       FloatingActionMenu child,
                                       View directTargetChild,
                                       View target,
                                       int nestedScrollAxes) {
        return nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL;
    }

    @Override
    public void onNestedScroll(CoordinatorLayout coordinatorLayout,
                               FloatingActionMenu child,
                               View target,
                               int dxConsumed,
                               int dyConsumed,
                               int dxUnconsumed,
                               int dyUnconsumed) {
        if (dyConsumed > 0) {
            // User scrolled down -> hide the FAB
            child.hideMenuButton(true);
        } else if (dyConsumed < 0) {
            // User scrolled up -> show the FAB
            child.showMenuButton(true);
        }
    }
}

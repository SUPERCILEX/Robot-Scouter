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
        if (dependency instanceof Snackbar.SnackbarLayout) {
            float translationY =
                    Math.min(0, ViewCompat.getTranslationY(dependency) - dependency.getHeight());
            ViewCompat.setTranslationY(child, translationY);
            return true;
        } else {
            return super.onDependentViewChanged(parent, child, dependency);
        }
    }

    @Override
    public void onDependentViewRemoved(CoordinatorLayout parent,
                                       FloatingActionMenu child,
                                       View dependency) {
        if (dependency instanceof Snackbar.SnackbarLayout) {
            float translationY =
                    Math.max(0, ViewCompat.getTranslationY(dependency) - dependency.getHeight());
            ViewCompat.setTranslationY(child, translationY);
        }
    }
}

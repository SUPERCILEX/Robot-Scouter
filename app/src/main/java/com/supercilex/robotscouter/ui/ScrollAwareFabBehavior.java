package com.supercilex.robotscouter.ui;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;

public class ScrollAwareFabBehavior extends FloatingActionButton.Behavior {
    public ScrollAwareFabBehavior(Context context, AttributeSet attrs) { // NOPMD
        super();
        // Needed for xml layout inflation
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout,
                                       FloatingActionButton child,
                                       View directTargetChild,
                                       View target,
                                       int nestedScrollAxes) {
        // Ensure we react to vertical scrolling
        return super.onStartNestedScroll(coordinatorLayout,
                                         child,
                                         directTargetChild,
                                         target,
                                         nestedScrollAxes)
                || nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL;
    }

    @Override
    public void onNestedScroll(CoordinatorLayout coordinatorLayout,
                               FloatingActionButton child,
                               View target,
                               int dxConsumed,
                               int dyConsumed,
                               int dxUnconsumed,
                               int dyUnconsumed) {
        super.onNestedScroll(coordinatorLayout,
                             child,
                             target,
                             dxConsumed,
                             dyConsumed,
                             dxUnconsumed,
                             dyUnconsumed);

        if (dyConsumed > 0 && child.getVisibility() == View.VISIBLE) {
            // User scrolled down and the FAB is currently visible -> hide the FAB
            child.hide(new FloatingActionButton.OnVisibilityChangedListener() {
                @Override
                public void onHidden(FloatingActionButton fab) {
                    // TODO remove hacky listener in support lib 25.1.1
                    // See: http://stackoverflow.com/a/41150767/4548500
                    fab.setVisibility(View.INVISIBLE);
                }
            });
        } else if (dyConsumed < 0 && child.getVisibility() != View.VISIBLE) {
            // User scrolled up and the FAB is currently not visible -> show the FAB
            child.show();
        }
    }
}

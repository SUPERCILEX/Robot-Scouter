package com.supercilex.robotscouter.util.ui;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.view.ViewGroup;

/**
 * A PagerAdapter whose {@link #setPrimaryItem} is overridden with proper nullability annotations.
 */
public abstract class NullablePagerAdapter extends PagerAdapter {
    @Override
    public void setPrimaryItem(@NonNull ViewGroup container,
                               int position,
                               @Nullable Object object) {
        super.setPrimaryItem(container, position, object);
        // `object` is actually nullable. It's even in the dang source code which is hilariously
        // ridiculous:
        // `mAdapter.setPrimaryItem(this, mCurItem, curItem != null ? curItem.object : null);`
    }
}

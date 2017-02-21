package com.supercilex.robotscouter.ui.scout.viewholder;

import android.annotation.SuppressLint;
import android.support.annotation.CallSuper;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.database.DatabaseReference;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.metrics.ScoutMetric;

public abstract class ScoutViewHolderBase<TMetric, VView extends TextView> extends RecyclerView.ViewHolder {
    protected VView mName;
    protected ScoutMetric<TMetric> mMetric;
    protected DatabaseReference mRef;
    protected FragmentManager mManager;
    protected SimpleItemAnimator mAnimator;

    @SuppressLint("WrongViewCast")
    public ScoutViewHolderBase(View itemView) {
        super(itemView);
        //noinspection unchecked
        mName = (VView) itemView.findViewById(R.id.name);
    }

    public final void bind(ScoutMetric<TMetric> metric,
                           DatabaseReference ref,
                           FragmentManager manager,
                           SimpleItemAnimator animator) {
        mMetric = metric;
        mRef = ref;
        mManager = manager;
        mAnimator = animator;

        bind();
    }

    @CallSuper
    protected void bind() {
        mName.setText(mMetric.getName());
    }

    protected void updateMetricName(String name) {
        if (!TextUtils.equals(mMetric.getName(), name)) {
            mMetric.setName(mRef, name, mAnimator);
        }
    }

    protected void updateMetricValue(TMetric value) {
        if (!value.equals(mMetric.getValue())) {
            mMetric.setValue(mRef, value, mAnimator);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ScoutViewHolderBase<?, ?> base = (ScoutViewHolderBase<?, ?>) o;

        return mMetric.equals(base.mMetric);
    }

    @Override
    public int hashCode() {
        return mMetric.hashCode();
    }

    @Override
    public String toString() {
        return mMetric.toString();
    }
}

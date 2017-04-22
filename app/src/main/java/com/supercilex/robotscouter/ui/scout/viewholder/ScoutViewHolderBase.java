package com.supercilex.robotscouter.ui.scout.viewholder;

import android.annotation.SuppressLint;
import android.support.annotation.CallSuper;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.metrics.ScoutMetric;

public abstract class ScoutViewHolderBase<TMetric, VView extends TextView> extends RecyclerView.ViewHolder {
    protected final VView mName;
    protected ScoutMetric<TMetric> mMetric;
    protected FragmentManager mManager;

    private SimpleItemAnimator mAnimator;

    @SuppressLint("WrongViewCast")
    public ScoutViewHolderBase(View itemView) {
        super(itemView);
        //noinspection unchecked
        mName = (VView) itemView.findViewById(R.id.name);
    }

    public final void bind(ScoutMetric<TMetric> metric,
                           FragmentManager manager,
                           SimpleItemAnimator animator) {
        mMetric = metric;
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
            disableAnimations();
            mMetric.updateName(name);
        }
    }

    protected void updateMetricValue(TMetric value) {
        if (!value.equals(mMetric.getValue())) {
            disableAnimations();
            mMetric.updateValue(value);
        }
    }

    protected void disableAnimations() {
        mAnimator.setSupportsChangeAnimations(false);
    }

    @Override
    public String toString() {
        return mMetric.toString();
    }
}

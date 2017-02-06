package com.supercilex.robotscouter.ui.scout.viewholder;

import android.annotation.SuppressLint;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.database.Query;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.ScoutMetric;

public abstract class ScoutViewHolderBase<TMetric, VView extends TextView> extends RecyclerView.ViewHolder {
    protected VView mName;
    protected ScoutMetric<TMetric> mMetric;
    protected Query mQuery;
    protected FragmentManager mManager;
    protected SimpleItemAnimator mAnimator;

    @SuppressLint("WrongViewCast")
    public ScoutViewHolderBase(View itemView) {
        super(itemView);
        //noinspection unchecked
        mName = (VView) itemView.findViewById(R.id.name);
    }

    public void bind(ScoutMetric<TMetric> metric,
                     Query query,
                     FragmentManager manager,
                     SimpleItemAnimator animator) {
        mMetric = metric;
        mQuery = query;
        mManager = manager;
        mAnimator = animator;

        bind();
    }

    protected void bind() {
        mName.setText(mMetric.getName());
    }

    protected void updateMetricName(String name) {
        mMetric.setName(mQuery, name, mAnimator);
    }

    protected void updateMetricValue(TMetric value) {
        mMetric.setValue(mQuery, value, mAnimator);
    }
}

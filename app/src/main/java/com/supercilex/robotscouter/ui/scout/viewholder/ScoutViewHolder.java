package com.supercilex.robotscouter.ui.scout.viewholder;

import android.annotation.SuppressLint;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.database.Query;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.ScoutMetric;

public abstract class ScoutViewHolder<TMetric, VView extends TextView> extends RecyclerView.ViewHolder {
    protected VView mName;
    protected ScoutMetric<TMetric> mMetric;

    @SuppressLint("WrongViewCast")
    public ScoutViewHolder(View itemView) {
        super(itemView);
        mName = (VView) itemView.findViewById(R.id.name);
    }

    public void bind(ScoutMetric<TMetric> metric) {
        mMetric = metric;
        mName.setText(mMetric.getName());
    }

    public abstract void setClickListeners(final Query query,
                                           final SimpleItemAnimator animator);
}

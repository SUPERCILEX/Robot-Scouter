package com.supercilex.robotscouter.ui.scout.viewholder;

import android.annotation.SuppressLint;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.database.Query;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.ScoutMetric;

public abstract class ScoutViewHolder<TMetric, VView extends TextView> extends RecyclerView.ViewHolder {
    protected VView mName;

    @SuppressLint("WrongViewCast")
    public ScoutViewHolder(View itemView) {
        super(itemView);
        mName = (VView) itemView.findViewById(R.id.name);
    }

    public void bind(ScoutMetric<TMetric> metric, Query query) {
        mName.setText(metric.getName());
        setClickListeners(metric, query);
    }

    public abstract void setClickListeners(final ScoutMetric<TMetric> metric, final Query query);
}

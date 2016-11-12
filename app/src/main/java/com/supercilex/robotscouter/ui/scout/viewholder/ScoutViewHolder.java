package com.supercilex.robotscouter.ui.scout.viewholder;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.google.firebase.database.DatabaseReference;
import com.supercilex.robotscouter.data.model.ScoutMetric;

public abstract class ScoutViewHolder extends RecyclerView.ViewHolder {
    public ScoutViewHolder(View itemView) {
        super(itemView);
    }

    public abstract void bind(ScoutMetric view, DatabaseReference ref);
}

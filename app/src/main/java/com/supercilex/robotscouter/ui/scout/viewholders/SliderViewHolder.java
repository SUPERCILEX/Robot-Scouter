package com.supercilex.robotscouter.ui.scout.viewholders;

import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.firebase.database.DatabaseReference;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.ScoutMetric;

public class SliderViewHolder extends ScoutViewHolder {
    private TextView mName;
    private SeekBar mSeekBar;

    public SliderViewHolder(View itemView) {
        super(itemView);
        mName = (TextView) itemView.findViewById(R.id.current_scout_text_view);
        mSeekBar = (SeekBar) itemView.findViewById(R.id.seek_bar);
    }

    @Override
    public void initialize(ScoutMetric view, DatabaseReference ref) {

    }

    public void setText(String name) {

    }
}

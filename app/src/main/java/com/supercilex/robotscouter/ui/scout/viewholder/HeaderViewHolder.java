package com.supercilex.robotscouter.ui.scout.viewholder;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.supercilex.robotscouter.R;

public class HeaderViewHolder extends ScoutViewHolderBase {
    public HeaderViewHolder(View itemView) {
        super(itemView);
    }

    @Override
    protected void bind() {
        super.bind();
        if (getLayoutPosition() != 0) {
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) itemView.getLayoutParams();
            params.topMargin =
                    (int) itemView.getResources().getDimension(R.dimen.card_padding_vertical);
        }
    }
}

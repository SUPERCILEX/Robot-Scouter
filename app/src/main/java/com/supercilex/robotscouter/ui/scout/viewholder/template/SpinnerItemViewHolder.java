package com.supercilex.robotscouter.ui.scout.viewholder.template;

import android.support.annotation.Keep;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.EditText;

import com.google.firebase.database.Query;
import com.supercilex.robotscouter.R;

public class SpinnerItemViewHolder extends RecyclerView.ViewHolder implements ScoutTemplateViewHolder {
    private String mPrevText;

    private EditText mItemEditText;
    private Query mQuery;

    @Keep
    public SpinnerItemViewHolder(View itemView) {
        super(itemView);
        mItemEditText = (EditText) itemView.findViewById(R.id.name);
    }

    public void bind(String text, Query query) {
        mPrevText = text;
        mQuery = query;

        mItemEditText.setText(text);
        mItemEditText.setOnFocusChangeListener(this);
    }

    @Override
    public void requestFocus() {
        mItemEditText.requestFocus();
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        String text = mItemEditText.getText().toString();
        if (!hasFocus && !text.equals(mPrevText)) mQuery.getRef().setValue(text);
    }
}

package com.supercilex.robotscouter.ui.scout.viewholder.template;

import android.support.annotation.Keep;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import com.google.firebase.database.DataSnapshot;
import com.supercilex.robotscouter.R;

public class SpinnerItemViewHolder extends RecyclerView.ViewHolder implements ScoutTemplateViewHolder {
    private String mPrevText;

    private EditText mItemEditText;
    private DataSnapshot mSnapshot;

    @Keep
    public SpinnerItemViewHolder(View itemView) {
        super(itemView);
        mItemEditText = (EditText) itemView.findViewById(R.id.name);
    }

    public void bind(String text, DataSnapshot snapshot) {
        mPrevText = text;
        mSnapshot = snapshot;

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
        if (!hasFocus && !TextUtils.equals(text, mPrevText)) {
            mSnapshot.getRef().setValue(text, mSnapshot.getPriority());
        }
    }
}
